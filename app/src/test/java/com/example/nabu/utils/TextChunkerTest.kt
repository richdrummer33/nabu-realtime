package com.example.nabu.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TextChunkerTest {

    @Test
    fun `chunks text by word count`() {
        val text = "This is a simple sentence. This is another sentence. And here is a third one."
        val chunks = TextChunker.chunkByWords(text, maxWords = 10, minWords = 5)
        
        assertTrue("Should produce chunks", chunks.isNotEmpty())
        chunks.forEach { chunk ->
            val wordCount = chunk.trim().split(Regex("\\s+")).size
            assertTrue("Each chunk should have reasonable word count: $wordCount", wordCount <= 10)
        }
    }

    @Test
    fun `respects sentence boundaries`() {
        val text = "First sentence. Second sentence. Third sentence."
        val chunks = TextChunker.chunkByWords(text, maxWords = 5, minWords = 2)
        
        assertTrue("Should produce chunks", chunks.isNotEmpty())
        chunks.forEach { chunk ->
            assertTrue("Chunk should end with punctuation or be complete: '$chunk'", 
                chunk.matches(Regex(".*[.!?]\\s*")) || chunk == chunks.last())
        }
    }

    @Test
    fun `strips markdown formatting`() {
        val text = "This is **bold** and *italic* text with `code` and [link](url)."
        val chunks = TextChunker.chunkByWords(text, maxWords = 20, minWords = 5)
        
        assertTrue("Should produce chunks", chunks.isNotEmpty())
        val result = chunks.joinToString(" ")
        
        assertTrue("Should not contain markdown bold", !result.contains("**"))
        assertTrue("Should not contain markdown italic stars", !result.contains("*italic*"))
        assertTrue("Should not contain backticks", !result.contains("`"))
        assertTrue("Should not contain square brackets", !result.contains("["))
    }

    @Test
    fun `handles newlines`() {
        val text = "First line\nSecond line\r\nThird line\rFourth line"
        val chunks = TextChunker.chunkByWords(text, maxWords = 10, minWords = 1)
        
        assertTrue("Should produce chunks", chunks.isNotEmpty())
        assertTrue("Should handle all newline types", chunks.size >= 2)
    }

    @Test
    fun `handles oversized sentences`() {
        val longSentence = "This is a very long sentence " + "that repeats ".repeat(15) + "and exceeds the max word count."
        val chunks = TextChunker.chunkByWords(longSentence, maxWords = 10, minWords = 5)
        
        assertTrue("Should split oversized sentences", chunks.size > 1)
        chunks.forEach { chunk ->
            val wordCount = chunk.trim().split(Regex("\\s+")).size
            assertTrue("Each chunk should not greatly exceed max: $wordCount", wordCount <= 11) // Allow slight overflow
        }
    }

    @Test
    fun `returns empty list for blank text`() {
        val chunks1 = TextChunker.chunkByWords("", maxWords = 10, minWords = 5)
        val chunks2 = TextChunker.chunkByWords("   ", maxWords = 10, minWords = 5)
        
        assertTrue("Empty text should return empty list", chunks1.isEmpty())
        assertTrue("Blank text should return empty list", chunks2.isEmpty())
    }

    @Test
    fun `handles small text that fits in one chunk`() {
        val text = "Short text."
        val chunks = TextChunker.chunkByWords(text, maxWords = 10, minWords = 1)
        
        assertEquals("Small text should be one chunk", 1, chunks.size)
        assertEquals("Chunk should match original text", "Short text.", chunks[0])
    }

    @Test
    fun `preserves sentence endings`() {
        val text = "Question? Statement. Exclamation!"
        val chunks = TextChunker.chunkByWords(text, maxWords = 3, minWords = 1)
        
        assertTrue("Should produce chunks", chunks.isNotEmpty())
        val combined = chunks.joinToString(" ")
        assertTrue("Should preserve question mark", combined.contains("?"))
        assertTrue("Should preserve period", combined.contains("."))
        assertTrue("Should preserve exclamation", combined.contains("!"))
    }

    @Test
    fun `handles mixed markdown and newlines`() {
        val text = "# Header\n\nThis is **bold** text.\n\nAnd *italic* too."
        val chunks = TextChunker.chunkByWords(text, maxWords = 15, minWords = 3)
        
        assertTrue("Should produce chunks", chunks.isNotEmpty())
        val result = chunks.joinToString(" ")
        assertTrue("Should strip header marker", !result.contains("#"))
        assertTrue("Should strip markdown", !result.contains("**") && !result.contains("*italic*"))
    }
}
