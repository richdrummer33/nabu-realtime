package com.example.nabu.utils

import android.content.Context
import com.example.nabu.kokoro.RunEp

object SettingsManager {
    fun setDebug(context: Context, enabled: Boolean) {
        DatabaseManager.setSetting(context, "debug", if (enabled) "1" else "0")
    }

    fun isDebug(context: Context): Boolean =
        (DatabaseManager.getSetting(context, "debug") ?: "0") == "1"

    fun setBenchmark(context: Context, enabled: Boolean) {
        DatabaseManager.setSetting(context, "benchmark", if (enabled) "1" else "0")
    }

    fun isBenchmark(context: Context): Boolean =
        (DatabaseManager.getSetting(context, "benchmark") ?: "0") == "1"

    fun setStyle(context: Context, style: String) {
        DatabaseManager.setSetting(context, "style", style)
    }

    fun getStyle(context: Context, default: String = "af_sky"): String =
        DatabaseManager.getSetting(context, "style") ?: default

    fun setSpeed(context: Context, speed: Float) {
        DatabaseManager.setSetting(context, "speed", speed.toString())
    }

    fun getSpeed(context: Context, default: Float = 1.0f): Float =
        DatabaseManager.getSetting(context, "speed")?.toFloat() ?: default

    fun setTtsEnabled(context: Context, enabled: Boolean) {
        DatabaseManager.setSetting(context, "tts_enabled", if (enabled) "1" else "0")
    }

    fun isTtsEnabled(context: Context, default: Boolean = true): Boolean {
        val fallback = if (default) "1" else "0"
        return (DatabaseManager.getSetting(context, "tts_enabled") ?: fallback) == "1"
    }

    fun setRuntimePreference(context: Context, ep: RunEp) {
        DatabaseManager.setSetting(context, "kokoro_ep", ep.name)
    }

    fun getRuntimePreference(context: Context, default: RunEp = RunEp.AUTO): RunEp =
        DatabaseManager.getSetting(context, "kokoro_ep")?.let {
            runCatching { RunEp.valueOf(it) }.getOrNull()
        } ?: default

    fun setTtsEngine(context: Context, engine: String) {
        DatabaseManager.setSetting(context, "tts_engine", engine)
    }

    fun getTtsEngine(context: Context, default: String = "kokoro"): String =
        DatabaseManager.getSetting(context, "tts_engine") ?: default

    // Text preprocessing feature flags
    fun setExpandContractions(context: Context, enabled: Boolean) {
        DatabaseManager.setSetting(context, "expand_contractions", if (enabled) "1" else "0")
    }

    fun isExpandContractions(context: Context): Boolean =
        (DatabaseManager.getSetting(context, "expand_contractions") ?: "0") == "1"

    fun setHandleAcronyms(context: Context, enabled: Boolean) {
        DatabaseManager.setSetting(context, "handle_acronyms", if (enabled) "1" else "0")
    }

    fun isHandleAcronyms(context: Context): Boolean =
        (DatabaseManager.getSetting(context, "handle_acronyms") ?: "0") == "1"

    fun setUsePronunciationOverrides(context: Context, enabled: Boolean) {
        DatabaseManager.setSetting(context, "use_pronunciation_overrides", if (enabled) "1" else "0")
    }

    fun isUsePronunciationOverrides(context: Context): Boolean =
        (DatabaseManager.getSetting(context, "use_pronunciation_overrides") ?: "0") == "1"

    // Streaming audio feature flags
    fun setStreamingAudioEnabled(context: Context, enabled: Boolean) {
        DatabaseManager.setSetting(context, "streaming_audio_enabled", if (enabled) "1" else "0")
    }

    fun isStreamingAudioEnabled(context: Context): Boolean =
        (DatabaseManager.getSetting(context, "streaming_audio_enabled") ?: "0") == "1"

    // TTS metrics debugging
    fun setTtsMetricsEnabled(context: Context, enabled: Boolean) {
        DatabaseManager.setSetting(context, "tts_metrics_enabled", if (enabled) "1" else "0")
    }

    fun isTtsMetricsEnabled(context: Context): Boolean =
        (DatabaseManager.getSetting(context, "tts_metrics_enabled") ?: "0") == "1"
}
