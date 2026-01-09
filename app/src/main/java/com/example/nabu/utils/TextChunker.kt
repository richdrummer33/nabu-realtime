package com.example.nabu.utils

import com.example.nabu.utils.DebugLogger

object TextChunker {
    
    // Precompiled regex patterns for better performance
    private val MARKDOWN_BOLD_DOUBLE = Regex("\\*\\*(.+?)\\*\\*")
    private val MARKDOWN_ITALIC_SINGLE = Regex("\\*(.+?)\\*")
    private val MARKDOWN_BOLD_UNDERSCORE = Regex("__(.+?)__")
    private val MARKDOWN_ITALIC_UNDERSCORE = Regex("_(.+?)_")
    private val SENTENCE_BOUNDARY = Regex("([.!?]+\\s+|\n+)")
    private val WHITESPACE = Regex("\\s+")
    
    /**
     * Split text into chunks based on word count, respecting sentence boundaries
     * Handles newlines (\n, \r\n, \r) and markdown formatting
     */
    fun chunkByWords(
        text: String,
        maxWords: Int,
        minWords: Int
    ): List<String> {
        val chunks = mutableListOf<String>()
        
        try {
            // Normalize all newline types and clean markdown
            val normalized = text
                .replace("\r\n", "\n")
                .replace("\r", "\n")
                .replace(MARKDOWN_BOLD_DOUBLE, "$1")
                .replace(MARKDOWN_ITALIC_SINGLE, "$1")
                .replace(MARKDOWN_BOLD_UNDERSCORE, "$1")
                .replace(MARKDOWN_ITALIC_UNDERSCORE, "$1")
                .trim()
            
            if (normalized.isEmpty()) return emptyList()
            
            // Split on sentence boundaries and newlines
            val segments = normalized.split(SENTENCE_BOUNDARY).filter { it.isNotBlank() }
            
            var currentChunk = StringBuilder()
            var currentWordCount = 0
            
            for (segment in segments) {
                val segmentWords = countWords(segment)
                
                // If segment alone exceeds max, split it further
                if (segmentWords > maxWords) {
                    // Add current chunk if it exists
                    if (currentWordCount >= minWords) {
                        chunks.add(currentChunk.toString().trim())
                        currentChunk.clear()
                        currentWordCount = 0
                    }
                    
                    // Split oversized segment by words
                    chunks.addAll(splitLargeSegment(segment, maxWords, minWords))
                    continue
                }
                
                // Check if adding this segment would exceed max
                if (currentWordCount + segmentWords > maxWords && currentWordCount >= minWords) {
                    // Emit current chunk
                    chunks.add(currentChunk.toString().trim())
                    currentChunk.clear()
                    currentWordCount = 0
                }
                
                // Add segment to current chunk
                if (currentChunk.isNotEmpty()) {
                    currentChunk.append(" ")
                }
                currentChunk.append(segment.trim())
                currentWordCount += segmentWords
            }
            
            // Add remaining chunk if it meets minimum
            if (currentWordCount >= minWords) {
                chunks.add(currentChunk.toString().trim())
            } else if (currentChunk.isNotEmpty() && chunks.isNotEmpty()) {
                // Append to last chunk if below minimum
                chunks[chunks.lastIndex] = chunks.last() + " " + currentChunk.toString().trim()
            } else if (currentChunk.isNotEmpty()) {
                // Only chunk available, add it regardless of minimum
                chunks.add(currentChunk.toString().trim())
            }
            
        } catch (e: Exception) {
            DebugLogger.log("TextChunker error: ${e.message}")
            // Fallback: return original text as single chunk
            return listOf(text)
        }
        
        return chunks
    }
    
    private fun splitLargeSegment(segment: String, maxWords: Int, minWords: Int): List<String> {
        val result = mutableListOf<String>()
        val words = segment.split(WHITESPACE)
        
        var currentChunk = mutableListOf<String>()
        for (word in words) {
            currentChunk.add(word)
            if (currentChunk.size >= maxWords) {
                result.add(currentChunk.joinToString(" "))
                currentChunk.clear()
            }
        }
        
        if (currentChunk.isNotEmpty()) {
            if (currentChunk.size >= minWords || result.isEmpty()) {
                result.add(currentChunk.joinToString(" "))
            } else {
                // Append to last chunk
                result[result.lastIndex] = result.last() + " " + currentChunk.joinToString(" ")
            }
        }
        
        return result
    }
    
    private fun countWords(text: String): Int {
        return text.split(WHITESPACE).filter { it.isNotBlank() }.size
    }
}
