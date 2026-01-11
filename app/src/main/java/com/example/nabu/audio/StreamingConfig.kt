package com.example.nabu.audio

/**
 * Configuration for streaming audio playback.
 * All time values are in milliseconds unless otherwise specified.
 */
data class StreamingConfig(
    val initialBufferMs: Int = 200,      // Wait for this much audio before starting playback
    val lowWatermarkMs: Int = 80,        // Pause synthesis consumer when below this
    val highWatermarkMs: Int = 250,      // Resume when above this
    val targetChunkMs: Int = 50,         // Target inference chunk size
    val sampleRate: Int = 24000,         // Sample rate in Hz
    val enabled: Boolean = true          // Feature flag
) {
    /**
     * Convert milliseconds to frames at the configured sample rate.
     */
    fun msToFrames(ms: Int): Int {
        return (ms * sampleRate) / 1000
    }
    
    /**
     * Convert frames to milliseconds at the configured sample rate.
     */
    fun framesToMs(frames: Int): Int {
        return (frames * 1000) / sampleRate
    }
    
    /**
     * Get the initial buffer size in frames.
     */
    val initialBufferFrames: Int
        get() = msToFrames(initialBufferMs)
    
    /**
     * Get the low watermark in frames.
     */
    val lowWatermarkFrames: Int
        get() = msToFrames(lowWatermarkMs)
    
    /**
     * Get the high watermark in frames.
     */
    val highWatermarkFrames: Int
        get() = msToFrames(highWatermarkMs)
    
    /**
     * Get the target chunk size in frames.
     */
    val targetChunkFrames: Int
        get() = msToFrames(targetChunkMs)
}
