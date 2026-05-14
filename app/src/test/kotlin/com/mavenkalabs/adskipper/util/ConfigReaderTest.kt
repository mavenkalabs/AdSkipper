package com.mavenkalabs.adskipper.util

import junit.framework.TestCase.assertEquals
import org.junit.Assert
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.test.Test
import kotlin.test.fail

internal class ConfigReaderTest {

    @OptIn(ExperimentalAtomicApi::class)
    @Test
    fun verifyConfigRetrieval() {
        val callbackInvoked = AtomicBoolean(false)
        val latch = CountDownLatch(1)
        ConfigReader {
            callbackInvoked.store(true)
            latch.countDown()
        }.use {
            if (latch.await(2, TimeUnit.SECONDS)) {
                Assert.assertTrue(callbackInvoked.load())
            } else {
                fail("callback was not invoked")
            }
        }

    }

    @Test
    fun verifyConfigNotRetrievedWhenNoChange() {
        val callbackCount = AtomicInteger(0)
        val latch = CountDownLatch(1)
        ConfigReader {
                callbackCount.incrementAndGet()
                latch.countDown()
        }.use { configReader ->
            {
                if (latch.await(2, TimeUnit.SECONDS)) {
                    configReader.loadConfig()
                    assertEquals(1, callbackCount.get())
                } else {
                    fail("callback was not invoked")
                }
            }
        }
    }
}
