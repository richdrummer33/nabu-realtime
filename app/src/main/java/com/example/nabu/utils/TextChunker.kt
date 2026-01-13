package com.example.nabu.utils

import android.content.Context

/**
 * Utility for splitting text into chunks suitable for TTS synthesis.
 * Uses word-based chunking with configurable limits and respects sentence boundaries.
 */
object TextChunker {
    
    /**
     * Splits text into chunks based on word count, respecting sentence boundaries.
     * 
     * @param text The input text to chunk
     * @param context Android context for accessing settings
     * @return List of text chunks ready for TTS synthesis
     */
    fun chunkByWords(text: String, context: Context): List<String> {
        return try {
            val maxWords = SettingsManager.getMaxChunkWords(context)
            val minWords = SettingsManager.getMinChunkWords(context)
            chunkByWords(text, maxWords, minWords)
        } catch (e: Exception) {
            DebugLogger.log("TextChunker error: ${e.message}, returning original text")
            listOf(text)
        }
    }
    
    /**
     * Splits text into chunks based on word count with explicit limits.
     * 
     * @param text The input text to chunk
     * @param maxWords Maximum words per chunk
     * @param minWords Minimum words per chunk (for sentence boundary optimization)
     * @return List of text chunks
     */
    fun chunkByWords(text: String, maxWords: Int, minWords: Int): List<String> {
        if (text.isBlank()) return emptyList()
        
        // Normalize newlines and strip markdown
        val normalized = normalizeText(text)
        
        // Split into sentences
        val sentences = splitIntoSentences(normalized)
        
        if (sentences.isEmpty()) return emptyList()
        
        val chunks = mutableListOf<String>()
        val currentChunk = StringBuilder()
        var currentWordCount = 0
        
        for (sentence in sentences) {
            val sentenceWords = countWords(sentence)
            
            // If a single sentence exceeds maxWords, split it
            if (sentenceWords > maxWords) {
                // Add current chunk if it has content
                if (currentWordCount > 0) {
                    chunks.add(currentChunk.toString().trim())
                    currentChunk.clear()
                    currentWordCount = 0
                }
                // Split the oversized sentence
                chunks.addAll(splitOversizedSentence(sentence, maxWords))
                continue
            }
            
            // Check if adding this sentence would exceed maxWords
            if (currentWordCount > 0 && currentWordCount + sentenceWords > maxWords) {
                // Only commit the chunk if it meets minWords, otherwise keep building
                if (currentWordCount >= minWords) {
                    chunks.add(currentChunk.toString().trim())
                    currentChunk.clear()
                    currentWordCount = 0
                }
            }
            
            // Add sentence to current chunk
            if (currentChunk.isNotEmpty()) {
                currentChunk.append(" ")
            }
            currentChunk.append(sentence)
            currentWordCount += sentenceWords
            
            // If we've reached maxWords, commit the chunk
            if (currentWordCount >= maxWords) {
                chunks.add(currentChunk.toString().trim())
                currentChunk.clear()
                currentWordCount = 0
            }
        }
        
        // Add remaining content
        if (currentChunk.isNotEmpty()) {
            chunks.add(currentChunk.toString().trim())
        }
        
        return chunks.filter { it.isNotBlank() }
    }
    
    /**
     * Normalizes text by handling newlines and stripping markdown.
     */
    private fun normalizeText(text: String): String {
        // Replace escaped newlines and normalize all newline types
        var normalized = text.replace("\\n", "\n")
            .replace("/n", "\n")
            .replace("\r\n", "\n")
            .replace("\r", "\n")
        
        // Strip markdown formatting
        normalized = stripMarkdown(normalized)
        
        return normalized
    }
    
    /**
     * Strips common markdown formatting from text.
     */
    private fun stripMarkdown(text: String): String {
        var result = text
        
        // Remove bold: **text** or __text__
        result = result.replace(Regex("\\*\\*([^*]+)\\*\\*"), "$1")
        result = result.replace(Regex("__([^_]+)__"), "$1")
        
        // Remove italic: *text* or _text_
        result = result.replace(Regex("\\*([^*]+)\\*"), "$1")
        result = result.replace(Regex("_([^_]+)_"), "$1")
        
        // Remove inline code: `text`
        result = result.replace(Regex("`([^`]+)`"), "$1")
        
        // Remove links: [text](url) -> text
        result = result.replace(Regex("\\[([^\\]]+)\\]\\([^)]+\\)"), "$1")
        
        // Remove headers: # text (with multiline support)
        result = result.replace(Regex("(?m)^#{1,6}\\s+"), "")
        
        // Remove remaining special characters commonly used in markdown
        result = result.replace(Regex("[>#\\[\\](){}]"), "")
        
        return result
    }
    
    /**
     * Splits text into sentences, respecting common sentence endings.
     */
    private fun splitIntoSentences(text: String): List<String> {
        val sentences = mutableListOf<String>()
        val builder = StringBuilder()
        
        // Use regex to find sentence boundaries
        val regex = Regex("[.!?]+")
        var lastIndex = 0
        
        for (match in regex.findAll(text)) {
            val sentenceText = text.substring(lastIndex, match.range.last + 1)
            if (sentenceText.isNotBlank()) {
                sentences.add(sentenceText.trim())
            }
            lastIndex = match.range.last + 1
        }
        
        // Add any remaining text
        if (lastIndex < text.length) {
            val remaining = text.substring(lastIndex).trim()
            if (remaining.isNotEmpty()) {
                sentences.add(remaining)
            }
        }
        
        // Also split on newlines as sentence boundaries
        val result = mutableListOf<String>()
        for (sentence in sentences) {
            val lines = sentence.split("\n")
            for (line in lines) {
                val trimmed = line.trim()
                if (trimmed.isNotEmpty()) {
                    result.add(trimmed)
                }
            }
        }
        
        return result
    }
    
    /**
     * Splits an oversized sentence into smaller chunks.
     */
    private fun splitOversizedSentence(sentence: String, maxWords: Int): List<String> {
        val words = sentence.split(Regex("\\s+"))
        val chunks = mutableListOf<String>()
        
        var i = 0
        while (i < words.size) {
            val chunkWords = words.subList(i, minOf(i + maxWords, words.size))
            chunks.add(chunkWords.joinToString(" "))
            i += maxWords
        }
        
        return chunks
    }
    
    /**
     * Counts the number of words in a string.
     */
    private fun countWords(text: String): Int {
        if (text.isBlank()) return 0
        return text.trim().split(Regex("\\s+")).size
    }
}
