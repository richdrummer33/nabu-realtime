package com.example.nabu.utils

import org.junit.Assert.*
import org.junit.Test

class TextPreprocessorTest {

    @Test
    fun `normalizes whitespace`() {
        val config = TextPreprocessor.Config(normalizeWhitespace = true, normalizePunctuation = false)
        val input = "Hello   world  \n  multiple   spaces"
        val result = TextPreprocessor.preprocess(input, config)
        
        // Should collapse multiple spaces but preserve newlines in structure
        assertTrue(result.contains("Hello"))
        assertTrue(result.contains("world"))
        assertFalse(result.contains("   ")) // No triple spaces
    }

    @Test
    fun `expands common contractions`() {
        val config = TextPreprocessor.Config(
            expandContractions = true,
            normalizeWhitespace = false,
            normalizePunctuation = false
        )
        
        assertEquals("I am", TextPreprocessor.preprocess("I'm", config))
        assertEquals("do not", TextPreprocessor.preprocess("don't", config))
        assertEquals("will not", TextPreprocessor.preprocess("won't", config))
        assertEquals("cannot", TextPreprocessor.preprocess("can't", config))
        assertEquals("is not", TextPreprocessor.preprocess("isn't", config))
        assertEquals("have not", TextPreprocessor.preprocess("haven't", config))
    }

    @Test
    fun `expands contractions with proper case`() {
        val config = TextPreprocessor.Config(
            expandContractions = true,
            normalizeWhitespace = false,
            normalizePunctuation = false
        )
        
        assertEquals("Do not", TextPreprocessor.preprocess("Don't", config))
        assertEquals("I Am", TextPreprocessor.preprocess("I'M", config).trim())
    }

    @Test
    fun `expands will contractions`() {
        val config = TextPreprocessor.Config(
            expandContractions = true,
            normalizeWhitespace = false,
            normalizePunctuation = false
        )
        
        assertEquals("I will", TextPreprocessor.preprocess("I'll", config))
        assertEquals("you will", TextPreprocessor.preprocess("you'll", config))
        assertEquals("they will", TextPreprocessor.preprocess("they'll", config))
    }

    @Test
    fun `expands have contractions`() {
        val config = TextPreprocessor.Config(
            expandContractions = true,
            normalizeWhitespace = false,
            normalizePunctuation = false
        )
        
        assertEquals("I have", TextPreprocessor.preprocess("I've", config))
        assertEquals("you have", TextPreprocessor.preprocess("you've", config))
        assertEquals("they have", TextPreprocessor.preprocess("they've", config))
    }

    @Test
    fun `expands would contractions`() {
        val config = TextPreprocessor.Config(
            expandContractions = true,
            normalizeWhitespace = false,
            normalizePunctuation = false
        )
        
        assertEquals("I would", TextPreprocessor.preprocess("I'd", config))
        assertEquals("you would", TextPreprocessor.preprocess("you'd", config))
        assertEquals("they would", TextPreprocessor.preprocess("they'd", config))
    }

    @Test
    fun `handles acronyms by spacing them out`() {
        val config = TextPreprocessor.Config(
            handleAcronyms = true,
            normalizeWhitespace = false,
            normalizePunctuation = false
        )
        
        val result = TextPreprocessor.preprocess("NASA API", config)
        assertTrue(result.contains("N A S A"))
        assertTrue(result.contains("A P I"))
    }

    @Test
    fun `handles mixed length acronyms`() {
        val config = TextPreprocessor.Config(
            handleAcronyms = true,
            normalizeWhitespace = false,
            normalizePunctuation = false
        )
        
        // Should handle 2-5 character acronyms
        assertTrue(TextPreprocessor.preprocess("AI", config).contains("A I"))
        assertTrue(TextPreprocessor.preprocess("HTTP", config).contains("H T T P"))
        assertTrue(TextPreprocessor.preprocess("HTTPS", config).contains("H T T P S"))
        
        // Should NOT handle 6+ character acronyms (likely not acronyms)
        assertFalse(TextPreprocessor.preprocess("SOMETHING", config).contains("S O M E"))
    }

    @Test
    fun `does not handle acronyms when disabled`() {
        val config = TextPreprocessor.Config(
            handleAcronyms = false,
            normalizeWhitespace = false,
            normalizePunctuation = false
        )
        
        val result = TextPreprocessor.preprocess("NASA API", config)
        assertEquals("NASA API", result)
    }

    @Test
    fun `combines multiple preprocessing options`() {
        val config = TextPreprocessor.Config(
            expandContractions = true,
            handleAcronyms = true,
            normalizeWhitespace = true,
            normalizePunctuation = true
        )
        
        val input = "I'm using NASA's API"
        val result = TextPreprocessor.preprocess(input, config)
        
        // Should expand contraction and handle acronym
        assertTrue(result.contains("I am"))
        assertTrue(result.contains("N A S A"))
        assertTrue(result.contains("A P I"))
    }

    @Test
    fun `normalizes punctuation`() {
        val config = TextPreprocessor.Config(
            normalizeWhitespace = false,
            normalizePunctuation = true,
            expandContractions = false,
            handleAcronyms = false
        )
        
        // Test quote normalization
        val quotes = TextPreprocessor.preprocess("'hello' "world"", config)
        assertTrue(quotes.contains("'hello'"))
        assertTrue(quotes.contains("\"world\""))
        
        // Test title abbreviations
        assertTrue(TextPreprocessor.preprocess("Dr. Smith", config).contains("Doctor"))
        assertTrue(TextPreprocessor.preprocess("Mr. Jones", config).contains("Mister"))
        assertTrue(TextPreprocessor.preprocess("Ms. Williams", config).contains("Miss"))
    }

    @Test
    fun `default config has all features disabled`() {
        val config = TextPreprocessor.Config()
        
        assertFalse(config.expandContractions)
        assertFalse(config.handleAcronyms)
        assertTrue(config.normalizeWhitespace)
        assertTrue(config.normalizePunctuation)
    }

    @Test
    fun `handles empty string`() {
        val config = TextPreprocessor.Config(
            expandContractions = true,
            handleAcronyms = true
        )
        
        assertEquals("", TextPreprocessor.preprocess("", config))
    }

    @Test
    fun `handles text without contractions or acronyms`() {
        val config = TextPreprocessor.Config(
            expandContractions = true,
            handleAcronyms = true
        )
        
        val input = "This is a simple sentence."
        val result = TextPreprocessor.preprocess(input, config)
        
        // Should not break normal text
        assertTrue(result.contains("This"))
        assertTrue(result.contains("simple"))
        assertTrue(result.contains("sentence"))
    }
}
