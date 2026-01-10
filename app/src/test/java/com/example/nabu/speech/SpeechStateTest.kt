package com.example.nabu.speech

import org.junit.Test
import org.junit.Assert.*

class SpeechStateTest {
    
    @Test
    fun testIdleStatusString() {
        val state = SpeechState.Idle
        assertEquals("Ready", state.toStatusString())
    }
    
    @Test
    fun testPreparingModelsStatusString() {
        val state = SpeechState.PreparingModels
        assertEquals("Preparing models...", state.toStatusString())
    }
    
    @Test
    fun testChunkingStatusString() {
        val state = SpeechState.Chunking(5)
        assertEquals("Chunking text (5 chunks)...", state.toStatusString())
    }
    
    @Test
    fun testSynthesizingStatusString() {
        val state = SpeechState.Synthesizing(2, 5)
        assertEquals("Synthesizing 2/5...", state.toStatusString())
    }
    
    @Test
    fun testBufferingStatusString() {
        val state = SpeechState.Buffering
        assertEquals("Buffering...", state.toStatusString())
    }
    
    @Test
    fun testPlayingStatusString() {
        val state = SpeechState.Playing(3, 5)
        assertEquals("Playing 3/5", state.toStatusString())
    }
    
    @Test
    fun testPausedStatusString() {
        val state = SpeechState.Paused(2, 5)
        assertEquals("Paused 2/5", state.toStatusString())
    }
    
    @Test
    fun testErrorStatusString() {
        val state = SpeechState.Error("Test error")
        assertEquals("Error: Test error", state.toStatusString())
    }
}
