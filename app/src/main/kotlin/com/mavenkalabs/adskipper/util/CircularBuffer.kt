package com.mavenkalabs.adskipper.util

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class CircularBuffer<T>(private val maxSize: Int) {

    init {
        require(maxSize > 0) { "Size must be greater than 0" }
    }

    private val queueArray = arrayOfNulls<Any>(maxSize)
    private var front = 0
    private var rear = -1
    private var count = 0
    private val mutex = Mutex()

    /**
     * Inserts an item. If the queue is full, it overwrites the oldest item.
     * This is a suspending function to ensure coroutine safety without blocking threads.
     */
    suspend fun enqueue(item: T) {
        mutex.withLock {
            if (count == maxSize) {
                // Queue is full: advance front to overwrite the oldest item
                front = (front + 1) % maxSize
                count--
            }

            rear = (rear + 1) % maxSize
            queueArray[rear] = item
            count++
        }
    }

    /**
     * Removes and returns the oldest item. Returns null if empty.
     */
    suspend fun dequeue(): T? {
        mutex.withLock {
            if (count == 0) return null

            @Suppress("UNCHECKED_CAST")
            val item = queueArray[front] as T
            queueArray[front] = null // Clear reference to allow Garbage Collection
            front = (front + 1) % maxSize
            count--
            return item
        }
    }

    suspend fun isEmpty(): Boolean {
        mutex.withLock {
            return count == 0
        }
    }
}
