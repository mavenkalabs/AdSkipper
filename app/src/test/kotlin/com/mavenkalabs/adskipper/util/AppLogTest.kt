package com.mavenkalabs.adskipper.util

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.Mock
import org.mockito.Mockito.`when` as whenever
import org.mockito.MockitoAnnotations
import java.nio.file.Files


internal class AppLogTest {
    @Mock
    private lateinit var mockContext : Context

    @Mock
    private lateinit var mockResolver: ContentResolver
    private lateinit var mocksCloseable: AutoCloseable

    @Before
    fun setUp()  {
        mocksCloseable = MockitoAnnotations.openMocks(this)
        whenever(mockContext.contentResolver).thenReturn(mockResolver)
        whenever(mockResolver.insert(any(), any())).thenReturn(Uri.EMPTY)
        whenever(mockResolver.openOutputStream(any()))
                .thenReturn(Files.newOutputStream((Files.createTempFile("_adskipper_", null))))
    }

    @After
    fun tearDown() {
        AppLog.disableA11yLogging()

        mocksCloseable.close()
    }

    @Test
    fun verifyMessageLoggingWithParams() {
        AppLog.d("TAG", "This is a {0} test with {1} and {2}", "Super", null, 1)
    }


    @Test
    fun verifyExceptionLogging() {
        AppLog.e("TAG", "This is a {0} test with {1} and {2}", Exception(),"Super", null, 1)
    }

    @Test
    fun verifyMessageLoggingWithParamsWithContext() {
        AppLog.enableA11yLogging(mockContext)
        AppLog.d("TAG", "This is a {0} test with {1} and {2}", "Super", null, 1)
    }


    @Test
    fun verifyExceptionLoggingWithContext() {
        AppLog.enableA11yLogging(mockContext)
        AppLog.e("TAG", "This is a {0} test with {1} and {2}", Exception(),"Super", null, 1)
    }
}
