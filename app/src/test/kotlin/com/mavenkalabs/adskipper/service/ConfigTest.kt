package com.mavenkalabs.adskipper.service

import android.content.Context
import android.content.res.AssetManager
import org.junit.After
import org.junit.Before
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import java.io.File
import java.lang.AutoCloseable
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

internal class ConfigTest {

    @Mock
    private lateinit var contextMock : Context

    @Mock
    private lateinit var assetManagerMock: AssetManager

    private lateinit var closeable: AutoCloseable

    @Before
    fun setupBefore() {
        closeable = MockitoAnnotations.openMocks(this)

        whenever { contextMock.assets }.thenReturn(assetManagerMock)
        whenever { assetManagerMock.open(any()) }.thenReturn(
            Files.newInputStream(
                File(listOf("src", "main", "assets", "config.json")
                    .joinToString(File.separator)).toPath()
            )
        )
    }

    @After
    fun tearDownAfter() {
        closeable.close()
    }

    @Test
    fun verifyLoading() {
        val cfg = loadConfig(contextMock)
        assertNotNull(cfg)
        assertTrue { cfg.size == 2 }
    }
}