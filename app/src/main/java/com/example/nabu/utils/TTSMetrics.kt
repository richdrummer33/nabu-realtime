package com.example.nabu.utils

import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

/**
 * Debug metrics for TTS synthesis and playback performance.
 * Thread-safe using atomic variables.
 */
object TTSMetrics {
    @Volatile
    var enabled: Boolean = false

    // Timing metrics (in milliseconds)
    private val timeToFirstAudio = AtomicLong(0)
    private val lastChunkInference = AtomicLong(0)
    private val totalInferenceTime = AtomicLong(0)
    private val inferenceCount = AtomicInteger(0)
    private val worstChunkInference = AtomicLong(0)

    // Buffer metrics
    private val currentBufferFill = AtomicInteger(0)
    private val peakBufferFill = AtomicInteger(0)

    // Underrun metrics
    private val underrunCount = AtomicInteger(0)
    private val totalUnderrunMs = AtomicLong(0)

    /**
     * Reset all metrics to zero.
     */
    fun reset() {
        timeToFirstAudio.set(0)
        lastChunkInference.set(0)
        totalInferenceTime.set(0)
        inferenceCount.set(0)
        worstChunkInference.set(0)
        currentBufferFill.set(0)
        peakBufferFill.set(0)
        underrunCount.set(0)
        totalUnderrunMs.set(0)
    }

    /**
     * Record an inference duration.
     */
    fun recordInference(durationMs: Long) {
        if (!enabled) return

        lastChunkInference.set(durationMs)
        totalInferenceTime.addAndGet(durationMs)
        inferenceCount.incrementAndGet()

        // Update worst case
        var current = worstChunkInference.get()
        while (durationMs > current) {
            if (worstChunkInference.compareAndSet(current, durationMs)) {
                break
            }
            current = worstChunkInference.get()
        }
    }

    /**
     * Record time to first audio playback.
     */
    fun recordFirstAudio(elapsedSinceSpeakMs: Long) {
        if (!enabled) return
        timeToFirstAudio.set(elapsedSinceSpeakMs)
    }

    /**
     * Record an audio underrun event.
     */
    fun recordUnderrun(durationMs: Long) {
        if (!enabled) return
        underrunCount.incrementAndGet()
        totalUnderrunMs.addAndGet(durationMs)
    }

    /**
     * Update buffer fill level.
     */
    fun updateBufferFill(fillFrames: Int) {
        if (!enabled) return
        
        currentBufferFill.set(fillFrames)
        
        // Update peak
        var current = peakBufferFill.get()
        while (fillFrames > current) {
            if (peakBufferFill.compareAndSet(current, fillFrames)) {
                break
            }
            current = peakBufferFill.get()
        }
    }

    /**
     * Take a snapshot of current metrics.
     */
    fun snapshot(): MetricsSnapshot {
        return MetricsSnapshot(
            timeToFirstAudioMs = timeToFirstAudio.get(),
            lastChunkInferenceMs = lastChunkInference.get(),
            avgChunkInferenceMs = if (inferenceCount.get() > 0) {
                totalInferenceTime.get().toDouble() / inferenceCount.get()
            } else {
                0.0
            },
            worstChunkInferenceMs = worstChunkInference.get(),
            currentBufferFillFrames = currentBufferFill.get(),
            peakBufferFillFrames = peakBufferFill.get(),
            underrunCount = underrunCount.get(),
            totalUnderrunMs = totalUnderrunMs.get(),
            inferenceCount = inferenceCount.get()
        )
    }

    /**
     * Immutable snapshot of metrics.
     */
    data class MetricsSnapshot(
        val timeToFirstAudioMs: Long,
        val lastChunkInferenceMs: Long,
        val avgChunkInferenceMs: Double,
        val worstChunkInferenceMs: Long,
        val currentBufferFillFrames: Int,
        val peakBufferFillFrames: Int,
        val underrunCount: Int,
        val totalUnderrunMs: Long,
        val inferenceCount: Int
    ) {
        override fun toString(): String {
            return """
                TTS Metrics:
                  Time to first audio: ${timeToFirstAudioMs}ms
                  Inference count: $inferenceCount
                  Last chunk inference: ${lastChunkInferenceMs}ms
                  Average chunk inference: ${"%.1f".format(avgChunkInferenceMs)}ms
                  Worst chunk inference: ${worstChunkInferenceMs}ms
                  Current buffer fill: $currentBufferFillFrames frames
                  Peak buffer fill: $peakBufferFillFrames frames
                  Underruns: $underrunCount (${totalUnderrunMs}ms total)
            """.trimIndent()
        }
    }
}
