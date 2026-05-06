package com.mavenkalabs.adskipper.util;

import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class ConfigReaderTest {

    @Test()
    public void verifyConfigRetrieval() throws Exception {
        final AtomicBoolean callbackInvoked = new AtomicBoolean();
        CountDownLatch latch = new CountDownLatch(1);
        try (final ConfigReader ignored = new ConfigReader(config -> {
            callbackInvoked.set(true);
            latch.countDown();
        })){
            if (latch.await(2, TimeUnit.SECONDS)) {
                Assert.assertTrue(callbackInvoked.get());
            } else {
                fail("callback was not invoked");
            }
        }
    }


    @Test()
    public void verifyConfigNotRetrievedWhenNoChange() throws Exception {
        final AtomicInteger callbackCount = new AtomicInteger();
        CountDownLatch latch = new CountDownLatch(1);
        try (ConfigReader configReader = new ConfigReader(config -> {
            callbackCount.incrementAndGet();
            latch.countDown();
        })){
            if (latch.await(2, TimeUnit.SECONDS)) {
                configReader.loadConfig();
                assertEquals(1, callbackCount.get());
            } else {
                fail("callback was not invoked");
            }

        }
    }
}
