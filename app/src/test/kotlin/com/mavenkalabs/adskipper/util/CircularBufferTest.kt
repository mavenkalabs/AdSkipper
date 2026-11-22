package com.mavenkalabs.adskipper.util

import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CircularBufferTest {
    @Test
    fun verifyEnqueueDeque() {
        runTest {
            val bufferSize = 10

            val buffer = CircularBuffer<String>(bufferSize)

            assertTrue(buffer.isEmpty())
            List(2*bufferSize) { "string$it"}.forEach { buffer.enqueue(it) }

            assertFalse(buffer.isEmpty())

            List(bufferSize) { buffer.dequeue()}
            assertTrue(buffer.isEmpty())
        }
    }
}