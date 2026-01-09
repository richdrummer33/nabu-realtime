package com.example.nabu.utils

object TextChunker {
    
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
                .replace(Regex("\\*\\*(.+?)\\*\\*"), "$1") // Remove bold **text**
                .replace(Regex("\\*(.+?)\\*"), "$1")       // Remove italic *text*
                .replace(Regex("__(.+?)__"), "$1")         // Remove bold __text__
                .replace(Regex("_(.+?)_"), "$1")           // Remove italic _text_
                .trim()
            
            if (normalized.isEmpty()) return emptyList()
            
            // Split on sentence boundaries and newlines
            val sentenceRegex = Regex("([.!?]+\\s+|\n+)")
            val segments = normalized.split(sentenceRegex).filter { it.isNotBlank() }
            
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
        val words = segment.split(Regex("\\s+"))
        
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
        return text.split(Regex("\\s+")).filter { it.isNotBlank() }.size
    }
}
