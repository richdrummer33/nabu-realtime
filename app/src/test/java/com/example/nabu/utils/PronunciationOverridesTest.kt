package com.example.nabu.utils

import android.content.Context
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.MockitoJUnitRunner
import java.io.ByteArrayInputStream

@RunWith(MockitoJUnitRunner::class)
class PronunciationOverridesTest {

    @Mock
    private lateinit var mockContext: Context

    @Mock
    private lateinit var mockAssets: android.content.res.AssetManager

    private val testJson = """
        {
          "nabu": "nah boo",
          "kokoro": "koh koh roh",
          "ONNX": "on ex",
          "API": "A P I",
          "GitHub": "git hub"
        }
    """.trimIndent()

    @Before
    fun setup() {
        `when`(mockContext.assets).thenReturn(mockAssets)
        `when`(mockAssets.open("pronunciations.json"))
            .thenReturn(ByteArrayInputStream(testJson.toByteArray()))
    }

    @Test
    fun `loads overrides from JSON`() {
        val overrides = PronunciationOverrides(mockContext)
        
        assertEquals(5, overrides.size())
        assertTrue(overrides.hasOverride("nabu"))
        assertTrue(overrides.hasOverride("kokoro"))
        assertTrue(overrides.hasOverride("ONNX"))
    }

    @Test
    fun `applies overrides case-insensitively`() {
        val overrides = PronunciationOverrides(mockContext)
        
        // Test exact case
        assertEquals("nah boo", overrides.apply("nabu"))
        
        // Test different case
        assertEquals("nah boo", overrides.apply("Nabu"))
        assertEquals("nah boo", overrides.apply("NABU"))
    }

    @Test
    fun `applies overrides in sentences`() {
        val overrides = PronunciationOverrides(mockContext)
        
        val input = "I'm using nabu with kokoro and the ONNX runtime."
        val result = overrides.apply(input)
        
        assertTrue(result.contains("nah boo"))
        assertTrue(result.contains("koh koh roh"))
        assertTrue(result.contains("on ex"))
    }

    @Test
    fun `respects word boundaries`() {
        val overrides = PronunciationOverrides(mockContext)
        
        // "API" should match but not be replaced inside "CAPITAL"
        val input = "The API is available"
        val result = overrides.apply(input)
        
        assertTrue(result.contains("A P I"))
        assertFalse(result.contains("CAPAP IL"))
    }

    @Test
    fun `handles text without overrides`() {
        val overrides = PronunciationOverrides(mockContext)
        
        val input = "This is plain text without any special words."
        val result = overrides.apply(input)
        
        assertEquals(input, result)
    }

    @Test
    fun `getOverride returns correct replacement`() {
        val overrides = PronunciationOverrides(mockContext)
        
        assertEquals("nah boo", overrides.getOverride("nabu"))
        assertEquals("koh koh roh", overrides.getOverride("kokoro"))
        assertEquals("on ex", overrides.getOverride("ONNX"))
    }

    @Test
    fun `getOverride is case-insensitive`() {
        val overrides = PronunciationOverrides(mockContext)
        
        assertEquals("nah boo", overrides.getOverride("NABU"))
        assertEquals("nah boo", overrides.getOverride("Nabu"))
        assertEquals("nah boo", overrides.getOverride("nabu"))
    }

    @Test
    fun `hasOverride is case-insensitive`() {
        val overrides = PronunciationOverrides(mockContext)
        
        assertTrue(overrides.hasOverride("nabu"))
        assertTrue(overrides.hasOverride("NABU"))
        assertTrue(overrides.hasOverride("Nabu"))
        assertFalse(overrides.hasOverride("notfound"))
    }

    @Test
    fun `handles empty overrides gracefully`() {
        // Mock empty JSON
        `when`(mockAssets.open("pronunciations.json"))
            .thenReturn(ByteArrayInputStream("{}".toByteArray()))
        
        val overrides = PronunciationOverrides(mockContext)
        
        assertEquals(0, overrides.size())
        val input = "test input"
        assertEquals(input, overrides.apply(input))
    }
}
