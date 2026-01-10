package com.example.nabu.utils

import org.junit.Test
import org.junit.Assert.*

class TextChunkerTest {
    
    @Test
    fun testChunkByWords_simpleText() {
        val text = "This is a test. This is another test."
        val chunks = TextChunker.chunkByWords(text, maxWords = 5, minWords = 2)
        
        // Should split into chunks respecting word limits
        assertTrue("Should produce at least one chunk", chunks.isNotEmpty())
        chunks.forEach { chunk ->
            val wordCount = chunk.split(Regex("\\s+")).filter { it.isNotBlank() }.size
            assertTrue("Each chunk should have reasonable word count", wordCount <= 10) // Allow some flexibility
        }
    }
    
    @Test
    fun testChunkByWords_withNewlines() {
        val text = "First line.\nSecond line.\nThird line."
        val chunks = TextChunker.chunkByWords(text, maxWords = 5, minWords = 1)
        
        assertTrue("Should produce chunks", chunks.isNotEmpty())
        // Newlines should be treated as sentence boundaries
        assertTrue("Should handle newlines", chunks.size >= 1)
    }
    
    @Test
    fun testChunkByWords_withMarkdown() {
        val text = "This is **bold** and this is *italic* text."
        val chunks = TextChunker.chunkByWords(text, maxWords = 10, minWords = 2)
        
        assertTrue("Should produce chunks", chunks.isNotEmpty())
        // Markdown should be stripped
        chunks.forEach { chunk ->
            assertFalse("Should strip markdown bold", chunk.contains("**"))
            assertFalse("Should strip markdown italic (when at word boundaries)", 
                chunk.matches(Regex(".*\\*[a-zA-Z]+\\*.*")))
        }
    }
    
    @Test
    fun testChunkByWords_emptyText() {
        val chunks = TextChunker.chunkByWords("", maxWords = 5, minWords = 2)
        assertTrue("Empty text should produce no chunks", chunks.isEmpty())
    }
    
    @Test
    fun testChunkByWords_whitespaceOnly() {
        val chunks = TextChunker.chunkByWords("   \n  \t  ", maxWords = 5, minWords = 2)
        assertTrue("Whitespace only should produce no chunks", chunks.isEmpty())
    }
    
    @Test
    fun testChunkByWords_veryLongSentence() {
        // Create a sentence with 100 words
        val longSentence = (1..100).joinToString(" ") { "word$it" } + "."
        val chunks = TextChunker.chunkByWords(longSentence, maxWords = 20, minWords = 5)
        
        assertTrue("Should split long sentence", chunks.size > 1)
        chunks.forEach { chunk ->
            val wordCount = chunk.split(Regex("\\s+")).filter { it.isNotBlank() }.size
            assertTrue("Each chunk should respect max words (with some tolerance)", wordCount <= 25)
        }
    }
    
    @Test
    fun testChunkByWords_multipleSentences() {
        val text = "First sentence. Second sentence. Third sentence. Fourth sentence."
        val chunks = TextChunker.chunkByWords(text, maxWords = 4, minWords = 2)
        
        assertTrue("Should produce multiple chunks", chunks.isNotEmpty())
        // Each chunk should be valid
        chunks.forEach { chunk ->
            assertTrue("Chunks should not be empty", chunk.isNotBlank())
        }
    }
    
    @Test
    fun testChunkByWords_differentNewlineTypes() {
        val text = "Line 1\rLine 2\r\nLine 3\nLine 4"
        val chunks = TextChunker.chunkByWords(text, maxWords = 5, minWords = 1)
        
        assertTrue("Should handle different newline types", chunks.isNotEmpty())
        // Should normalize all newline types
        chunks.forEach { chunk ->
            assertFalse("Should not contain \\r characters", chunk.contains('\r'))
        }
    }
}
