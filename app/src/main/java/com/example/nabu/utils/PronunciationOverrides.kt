package com.example.nabu.utils

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.IOException

/**
 * Manages custom pronunciation overrides loaded from assets/pronunciations.json.
 * Provides word-level replacements before phonemization.
 */
class PronunciationOverrides(context: Context) {

    private val overrides: Map<String, String>

    init {
        overrides = loadOverrides(context)
    }

    /**
     * Load pronunciation overrides from assets/pronunciations.json.
     * Returns an empty map if the file doesn't exist or can't be parsed.
     */
    private fun loadOverrides(context: Context): Map<String, String> {
        return try {
            val json = context.assets.open("pronunciations.json").bufferedReader().use { it.readText() }
            val type = object : TypeToken<Map<String, String>>() {}.type
            val loaded: Map<String, String> = Gson().fromJson(json, type)
            DebugLogger.log("PronunciationOverrides: Loaded ${loaded.size} overrides")
            loaded
        } catch (e: IOException) {
            DebugLogger.log("PronunciationOverrides: No pronunciations.json found, using empty overrides")
            emptyMap()
        } catch (e: Exception) {
            DebugLogger.log("PronunciationOverrides: Error loading pronunciations.json: ${e.message}")
            emptyMap()
        }
    }

    /**
     * Apply pronunciation overrides to the given text.
     * Performs case-insensitive word-level replacement.
     */
    fun apply(text: String): String {
        if (overrides.isEmpty()) {
            return text
        }

        var result = text

        // Sort by length (longest first) to handle multi-word overrides correctly
        val sortedOverrides = overrides.entries.sortedByDescending { it.key.length }

        for ((word, replacement) in sortedOverrides) {
            // Create case-insensitive regex with word boundaries
            val pattern = Regex("\\b${Regex.escape(word)}\\b", RegexOption.IGNORE_CASE)
            result = pattern.replace(result, replacement)
        }

        return result
    }

    /**
     * Get the number of loaded overrides.
     */
    fun size(): Int = overrides.size

    /**
     * Check if a specific word has an override.
     */
    fun hasOverride(word: String): Boolean {
        return overrides.keys.any { it.equals(word, ignoreCase = true) }
    }

    /**
     * Get the override for a specific word (case-insensitive).
     */
    fun getOverride(word: String): String? {
        return overrides.entries.find { it.key.equals(word, ignoreCase = true) }?.value
    }
}
