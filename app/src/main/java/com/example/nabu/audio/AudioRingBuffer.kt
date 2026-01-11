package com.example.nabu.audio

import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.min

/**
 * Lock-free ring buffer for streaming audio data.
 * Designed for single-producer, single-consumer scenarios (SPSC).
 * 
 * Thread-safe for concurrent reads and writes without locks.
 * Uses atomic integers for read/write positions.
 * 
 * Note: The buffer reserves one frame to distinguish between full and empty states.
 * This means the effective capacity is capacityFrames - 1.
 * 
 * @param capacityFrames Maximum number of frames the buffer can hold
 * @param frameSize Size of each frame in bytes (e.g., 2 for 16-bit mono)
 */
class AudioRingBuffer(
    val capacityFrames: Int,
    val frameSize: Int = 2  // 16-bit mono = 2 bytes per frame
) {
    // Pre-allocated buffer
    private val buffer: ByteArray = ByteArray(capacityFrames * frameSize)
    
    // Atomic positions for lock-free access
    // These track byte offsets, not frame offsets
    private val writePosition = AtomicInteger(0)
    private val readPosition = AtomicInteger(0)
    
    /**
     * Write data to the buffer.
     * Returns the number of frames actually written.
     * May write fewer than requested if buffer is full.
     */
    fun write(data: ByteArray, offset: Int = 0, length: Int = data.size): Int {
        val framesToWrite = length / frameSize
        val available = availableToWrite()
        val actualFrames = min(framesToWrite, available)
        
        if (actualFrames == 0) {
            return 0
        }
        
        val bytesToWrite = actualFrames * frameSize
        val writePos = writePosition.get()
        val bufferSize = buffer.size
        
        // Calculate how much we can write before wrapping
        val writeEnd = writePos + bytesToWrite
        
        if (writeEnd <= bufferSize) {
            // Simple case: no wrap-around
            System.arraycopy(data, offset, buffer, writePos, bytesToWrite)
        } else {
            // Wrap-around case: write in two parts
            val firstChunk = bufferSize - writePos
            val secondChunk = bytesToWrite - firstChunk
            
            System.arraycopy(data, offset, buffer, writePos, firstChunk)
            System.arraycopy(data, offset + firstChunk, buffer, 0, secondChunk)
        }
        
        // Update write position (with wrap-around)
        writePosition.set((writePos + bytesToWrite) % bufferSize)
        
        return actualFrames
    }
    
    /**
     * Read data from the buffer.
     * Returns the number of frames actually read.
     * May read fewer than requested if buffer doesn't have enough data.
     */
    fun read(dest: ByteArray, offset: Int = 0, length: Int = dest.size): Int {
        val framesToRead = length / frameSize
        val available = availableToRead()
        val actualFrames = min(framesToRead, available)
        
        if (actualFrames == 0) {
            return 0
        }
        
        val bytesToRead = actualFrames * frameSize
        val readPos = readPosition.get()
        val bufferSize = buffer.size
        
        // Calculate how much we can read before wrapping
        val readEnd = readPos + bytesToRead
        
        if (readEnd <= bufferSize) {
            // Simple case: no wrap-around
            System.arraycopy(buffer, readPos, dest, offset, bytesToRead)
        } else {
            // Wrap-around case: read in two parts
            val firstChunk = bufferSize - readPos
            val secondChunk = bytesToRead - firstChunk
            
            System.arraycopy(buffer, readPos, dest, offset, firstChunk)
            System.arraycopy(buffer, 0, dest, offset + firstChunk, secondChunk)
        }
        
        // Update read position (with wrap-around)
        readPosition.set((readPos + bytesToRead) % bufferSize)
        
        return actualFrames
    }
    
    /**
     * Returns the number of frames available to read.
     */
    fun availableToRead(): Int {
        val writePos = writePosition.get()
        val readPos = readPosition.get()
        val bufferSize = buffer.size
        
        val available = if (writePos >= readPos) {
            writePos - readPos
        } else {
            bufferSize - readPos + writePos
        }
        
        return available / frameSize
    }
    
    /**
     * Returns the number of frames available to write.
     */
    fun availableToWrite(): Int {
        // Leave one frame empty to distinguish full from empty
        return capacityFrames - availableToRead() - 1
    }
    
    /**
     * Clear the buffer and reset positions.
     */
    fun clear() {
        writePosition.set(0)
        readPosition.set(0)
    }
    
    /**
     * Check if the buffer is empty.
     */
    fun isEmpty(): Boolean {
        return availableToRead() == 0
    }
    
    /**
     * Check if the buffer is full.
     */
    fun isFull(): Boolean {
        return availableToWrite() == 0
    }
    
    /**
     * Get the current fill level as a percentage (0.0 to 1.0).
     */
    fun fillLevel(): Float {
        return availableToRead().toFloat() / capacityFrames.toFloat()
    }
}
