package com.example.nabu.speech

import org.junit.Test
import org.junit.Assert.*

class TextChunkerTest {
    
    @Test
    fun testEmptyText() {
        val chunks = TextChunker.chunkText("")
        assertTrue(chunks.isEmpty())
    }
    
    @Test
    fun testBlankText() {
        val chunks = TextChunker.chunkText("   ")
        assertTrue(chunks.isEmpty())
    }
    
    @Test
    fun testShortText() {
        val chunks = TextChunker.chunkText("Hello world.")
        assertEquals(1, chunks.size)
        assertEquals("Hello world.", chunks[0])
    }
    
    @Test
    fun testMultipleSentences() {
        val text = "First sentence. Second sentence! Third sentence?"
        val chunks = TextChunker.chunkText(text)
        assertEquals(3, chunks.size)
        assertEquals("First sentence.", chunks[0])
        assertEquals("Second sentence!", chunks[1])
        assertEquals("Third sentence?", chunks[2])
    }
    
    @Test
    fun testLongSentenceSplitsByWords() {
        val longSentence = "This is a very long sentence " + "that ".repeat(100) + "needs to be split."
        val chunks = TextChunker.chunkText(longSentence, maxChunkLength = 100)
        assertTrue(chunks.size > 1)
        chunks.forEach { chunk ->
            assertTrue(chunk.length <= 100)
        }
    }
    
    @Test
    fun testChunksAreNotBlank() {
        val text = "First.  Second.   Third."
        val chunks = TextChunker.chunkText(text)
        chunks.forEach { chunk ->
            assertTrue(chunk.isNotBlank())
        }
    }
}
