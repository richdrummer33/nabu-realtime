package com.example.nabu.audio

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class AudioRingBufferTest {

    private lateinit var buffer: AudioRingBuffer
    private val frameSize = 2  // 16-bit mono
    private val capacity = 100  // frames

    @Before
    fun setup() {
        buffer = AudioRingBuffer(capacityFrames = capacity, frameSize = frameSize)
    }

    @Test
    fun `starts empty`() {
        assertTrue(buffer.isEmpty())
        assertFalse(buffer.isFull())
        assertEquals(0, buffer.availableToRead())
        assertEquals(capacity - 1, buffer.availableToWrite())  // One frame reserved
        assertEquals(0.0f, buffer.fillLevel(), 0.001f)
    }

    @Test
    fun `writes and reads single frame`() {
        val data = ByteArray(frameSize) { it.toByte() }
        
        val written = buffer.write(data)
        assertEquals(1, written)
        assertEquals(1, buffer.availableToRead())
        
        val readData = ByteArray(frameSize)
        val read = buffer.read(readData)
        assertEquals(1, read)
        assertArrayEquals(data, readData)
        
        assertTrue(buffer.isEmpty())
    }

    @Test
    fun `writes and reads multiple frames`() {
        val frames = 10
        val data = ByteArray(frames * frameSize) { it.toByte() }
        
        val written = buffer.write(data)
        assertEquals(frames, written)
        assertEquals(frames, buffer.availableToRead())
        
        val readData = ByteArray(frames * frameSize)
        val read = buffer.read(readData)
        assertEquals(frames, read)
        assertArrayEquals(data, readData)
    }

    @Test
    fun `handles wrap-around on write`() {
        // Fill buffer almost to capacity
        val data1 = ByteArray(50 * frameSize) { 1 }
        buffer.write(data1)
        
        // Read some to make space
        val readData = ByteArray(30 * frameSize)
        buffer.read(readData)
        
        // Write more to cause wrap-around
        val data2 = ByteArray(40 * frameSize) { 2 }
        val written = buffer.write(data2)
        assertEquals(40, written)
        
        // Verify we can read it all back
        assertEquals(60, buffer.availableToRead())  // 20 left from first + 40 from second
    }

    @Test
    fun `handles wrap-around on read`() {
        // Write some data
        val data1 = ByteArray(60 * frameSize) { 1 }
        buffer.write(data1)
        
        // Read part of it
        val readData1 = ByteArray(40 * frameSize)
        buffer.read(readData1)
        
        // Write more to wrap around
        val data2 = ByteArray(50 * frameSize) { 2 }
        buffer.write(data2)
        
        // Read everything in one go (should wrap around)
        val readData2 = ByteArray(70 * frameSize)
        val read = buffer.read(readData2)
        assertEquals(70, read)
    }

    @Test
    fun `respects capacity limit`() {
        val data = ByteArray((capacity + 10) * frameSize) { it.toByte() }
        
        // Try to write more than capacity
        val written = buffer.write(data)
        
        // Should only write up to capacity - 1 (one frame reserved)
        assertTrue(written < capacity)
        assertTrue(buffer.isFull())
        assertEquals(0, buffer.availableToWrite())
    }

    @Test
    fun `read returns zero when empty`() {
        val readData = ByteArray(10 * frameSize)
        val read = buffer.read(readData)
        assertEquals(0, read)
    }

    @Test
    fun `write returns zero when full`() {
        // Fill the buffer
        val fillData = ByteArray((capacity - 1) * frameSize) { 1 }
        buffer.write(fillData)
        
        assertTrue(buffer.isFull())
        
        // Try to write more
        val moreData = ByteArray(10 * frameSize) { 2 }
        val written = buffer.write(moreData)
        assertEquals(0, written)
    }

    @Test
    fun `clear resets buffer`() {
        val data = ByteArray(50 * frameSize) { it.toByte() }
        buffer.write(data)
        
        assertFalse(buffer.isEmpty())
        
        buffer.clear()
        
        assertTrue(buffer.isEmpty())
        assertEquals(0, buffer.availableToRead())
        assertEquals(capacity - 1, buffer.availableToWrite())
    }

    @Test
    fun `fill level returns correct percentage`() {
        assertEquals(0.0f, buffer.fillLevel(), 0.001f)
        
        val data = ByteArray(50 * frameSize)
        buffer.write(data)
        
        assertEquals(0.5f, buffer.fillLevel(), 0.01f)
    }

    @Test
    fun `handles partial reads`() {
        val data = ByteArray(50 * frameSize) { it.toByte() }
        buffer.write(data)
        
        // Read less than available
        val readData = ByteArray(20 * frameSize)
        val read = buffer.read(readData)
        assertEquals(20, read)
        assertEquals(30, buffer.availableToRead())
    }

    @Test
    fun `handles partial writes`() {
        // Fill most of buffer
        val data1 = ByteArray(90 * frameSize) { 1 }
        buffer.write(data1)
        
        // Try to write more than space available
        val data2 = ByteArray(20 * frameSize) { 2 }
        val written = buffer.write(data2)
        
        // Should only write what fits
        assertTrue(written < 20)
    }

    @Test
    fun `concurrent reads and writes maintain consistency`() {
        // Simulate producer-consumer pattern
        for (i in 0 until 10) {
            val data = ByteArray(10 * frameSize) { i.toByte() }
            buffer.write(data)
            
            val readData = ByteArray(5 * frameSize)
            buffer.read(readData)
            
            // Buffer should grow by 5 frames each iteration
            assertEquals((i + 1) * 5, buffer.availableToRead())
        }
    }

    @Test
    fun `write with offset and length`() {
        val data = ByteArray(100) { it.toByte() }
        val offset = 20
        val length = 10 * frameSize
        
        val written = buffer.write(data, offset, length)
        assertEquals(10, written)
        
        val readData = ByteArray(10 * frameSize)
        buffer.read(readData)
        
        // Verify correct data was written
        for (i in 0 until length) {
            assertEquals(data[offset + i], readData[i])
        }
    }

    @Test
    fun `read with offset and length`() {
        val data = ByteArray(10 * frameSize) { it.toByte() }
        buffer.write(data)
        
        val readBuffer = ByteArray(100)
        val offset = 20
        val length = 10 * frameSize
        
        val read = buffer.read(readBuffer, offset, length)
        assertEquals(10, read)
        
        // Verify correct data was read at offset
        for (i in 0 until length) {
            assertEquals(data[i], readBuffer[offset + i])
        }
    }

    @Test
    fun `handles zero-length operations`() {
        val data = ByteArray(0)
        
        assertEquals(0, buffer.write(data))
        assertEquals(0, buffer.read(data))
    }
}
