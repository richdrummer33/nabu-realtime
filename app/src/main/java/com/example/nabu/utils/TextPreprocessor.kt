package com.example.nabu.utils

/**
 * Centralized text preprocessing for TTS pipeline.
 * Handles normalization, contraction expansion, number processing, and acronym handling.
 */
object TextPreprocessor {

    /**
     * Configuration for text preprocessing behavior.
     */
    data class Config(
        val expandContractions: Boolean = false,
        val handleAcronyms: Boolean = false,
        val normalizeWhitespace: Boolean = true,
        val normalizePunctuation: Boolean = true,
        val applySymbolDictionary: Boolean = true  // Control SymbolDictionary.replaceSymbols() separately
    )

    /**
     * Common English contractions mapped to their expanded forms.
     */
    private val contractionMap = mapOf(
        // Negative contractions
        "don't" to "do not",
        "doesn't" to "does not",
        "didn't" to "did not",
        "won't" to "will not",
        "can't" to "cannot",
        "couldn't" to "could not",
        "wouldn't" to "would not",
        "shouldn't" to "should not",
        "isn't" to "is not",
        "aren't" to "are not",
        "wasn't" to "was not",
        "weren't" to "were not",
        "haven't" to "have not",
        "hasn't" to "has not",
        "hadn't" to "had not",
        
        // Pronoun + verb contractions
        "I'm" to "I am",
        "you're" to "you are",
        "he's" to "he is",
        "she's" to "she is",
        "it's" to "it is",
        "we're" to "we are",
        "they're" to "they are",
        "that's" to "that is",
        "what's" to "what is",
        "there's" to "there is",
        "who's" to "who is",
        "where's" to "where is",
        
        // Will contractions
        "I'll" to "I will",
        "you'll" to "you will",
        "he'll" to "he will",
        "she'll" to "she will",
        "it'll" to "it will",
        "we'll" to "we will",
        "they'll" to "they will",
        
        // Have contractions
        "I've" to "I have",
        "you've" to "you have",
        "we've" to "we have",
        "they've" to "they have",
        
        // Would contractions
        "I'd" to "I would",
        "you'd" to "you would",
        "he'd" to "he would",
        "she'd" to "she would",
        "it'd" to "it would",
        "we'd" to "we would",
        "they'd" to "they would",
        
        // Other common contractions
        "let's" to "let us"
    )

    /**
     * Preprocess text with the given configuration.
     * This is the main entry point for text preprocessing.
     */
    fun preprocess(text: String, config: Config = Config()): String {
        var result = text

        // Normalize whitespace and punctuation
        if (config.normalizeWhitespace || config.normalizePunctuation) {
            result = normalizeText(result, config)
        }

        // Expand contractions
        if (config.expandContractions) {
            result = expandContractions(result)
        }

        // Handle acronyms
        if (config.handleAcronyms) {
            result = handleAcronyms(result)
        }

        return result
    }

    /**
     * Normalize whitespace and punctuation in text.
     * Based on logic from PhonemeConverter.normalizeText().
     */
    private fun normalizeText(text: String, config: Config): String {
        var normalized = text

        if (config.normalizeWhitespace) {
            // Normalize line breaks and trim each line
            normalized = normalized
                .lines()
                .joinToString("\n") { it.trim() }
        }

        if (config.normalizePunctuation) {
            // Normalize various quote styles to standard quotes
            normalized = normalized
                .replace("['']".toRegex(), "'")
                .replace("""[""«»]""".toRegex(), "\"")
                .replace("[、。！，：；？]".toRegex()) { match ->
                    when (match.value) {
                        "、" -> ","
                        "。" -> "."
                        "！" -> "!"
                        "，" -> ","
                        "：" -> ":"
                        "；" -> ";"
                        "？" -> "?"
                        else -> match.value
                    } + " "
                }

            // Normalize common abbreviations
            normalized = normalized
                .replace(Regex("\\bD[Rr]\\.(?=\\s|$)"), "Doctor")
                .replace(Regex("\\b(?:Mr\\.|MR\\.)(?=\\s|$)"), "Mister")
                .replace(Regex("\\b(?:Ms\\.|MS\\.)(?=\\s|$)"), "Miss")
                .replace(Regex("\\b(?:Mrs\\.|MRS\\.)(?=\\s|$)"), "Mrs")
                .replace(Regex("\\betc\\.(?!\\s*[A-Z])"), "etc")

            // Remove commas in numbers (e.g., 1,000 -> 1000)
            normalized = normalized.replace(Regex("(?<=\\d),(?=\\d)"), "")
            
            // Convert number ranges (e.g., 1-10 -> 1 to 10)
            normalized = normalized.replace(Regex("(?<=\\d)-(?=\\d)"), " to ")
        }

        // Apply symbol dictionary replacements
        if (config.applySymbolDictionary) {
            normalized = SymbolDictionary.replaceSymbols(normalized)
        }

        if (config.normalizeWhitespace) {
            // Collapse multiple spaces to single space
            normalized = normalized.replace(Regex("\\s+"), " ")
        }

        return normalized.trim()
    }

    /**
     * Expand contractions to their full forms.
     * Handles case-sensitive matching (e.g., "Don't" -> "Do not").
     */
    private fun expandContractions(text: String): String {
        var result = text

        // Sort by length (longest first) to handle overlapping patterns correctly
        val sortedContractions = contractionMap.entries.sortedByDescending { it.key.length }

        for ((contraction, expansion) in sortedContractions) {
            // Create case-insensitive regex with word boundaries
            val pattern = Regex("\\b${Regex.escape(contraction)}\\b", RegexOption.IGNORE_CASE)
            
            result = pattern.replace(result) { matchResult ->
                val matched = matchResult.value
                // Preserve case: all-caps -> capitalize all words, first-cap -> capitalize first char
                when {
                    matched.isEmpty() -> expansion
                    matched.all { it.isUpperCase() || !it.isLetter() } -> {
                        // All caps input: capitalize each word in expansion
                        expansion.split(" ").joinToString(" ") { word ->
                            word.replaceFirstChar { it.uppercase() }
                        }
                    }
                    matched[0].isUpperCase() -> {
                        // First character uppercase: capitalize first character of expansion
                        expansion.replaceFirstChar { it.uppercase() }
                    }
                    else -> expansion
                }
            }
        }

        return result
    }

    /**
     * Handle acronyms by spacing out all-caps words of 5 characters or less.
     * Examples: "NASA" -> "N A S A", "API" -> "A P I"
     */
    private fun handleAcronyms(text: String): String {
        // Match all-caps words of 2-5 characters
        val acronymPattern = Regex("\\b[A-Z]{2,5}\\b")
        
        return acronymPattern.replace(text) { matchResult ->
            val acronym = matchResult.value
            // Space out the letters
            acronym.toCharArray().joinToString(" ")
        }
    }
}
