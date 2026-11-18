package com.mavenkalabs.adskipper.util

import android.content.Context
import android.net.Uri
import com.mavenkalabs.adskipper.voice.VoiceModelManager
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.vosk.Model
import java.io.File
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

internal class VoiceModelManagerTest {
    @Mock
    private lateinit var mockContext : Context
    private lateinit var mocksCloseable: AutoCloseable

    private lateinit var modelParentDir: File

    private val modelName = "vosk-model-small-en-us-0.15"
    private val tmpDir = File(System.getProperty("java.io.tmpdir")?:"/tmp")

    @Before
    fun setUp()  {
        mocksCloseable = MockitoAnnotations.openMocks(this)

        modelParentDir = File(tmpDir, UUID.randomUUID().toString())
        modelParentDir.mkdirs()

        whenever { mockContext.filesDir }.doReturn(modelParentDir)
    }

    @After
    fun tearDown() {
        modelParentDir.deleteRecursively()

        mocksCloseable.close()
    }

    @Test
    fun verifyModelFetch() {
        runTest {
            val testDispatcher = StandardTestDispatcher(testScheduler)

            val mockUri = mock<Uri> {
                on { lastPathSegment } doReturn "$modelName.zip"
            }

            val mockedUri = Mockito.mockStatic(Uri::class.java)
            mockedUri.use { mockedUri ->
                mockedUri.`when`<Uri> { Uri.parse(any()) } doReturn mockUri
                Mockito.mockConstruction(Model::class.java).use { _ ->
                    assertNotNull(VoiceModelManager.getOrFetchModel(mockContext, ioDispatcher = testDispatcher))
                }
            }
        }
    }

    @Test
    fun verifyModelFetchFromCache() {
        runTest {
            val testDispatcher = StandardTestDispatcher(testScheduler)

            val mockUri = mock<Uri> {
                on { lastPathSegment } doReturn "$modelName.zip"
            }

            val mockedUri = Mockito.mockStatic(Uri::class.java)
            mockedUri.use { mockedUri ->
                mockedUri.`when`<Uri> { Uri.parse(any()) } doReturn mockUri
                Mockito.mockConstruction(Model::class.java).use { mockedConstruction ->
                    VoiceModelManager.getOrFetchModel(mockContext, ioDispatcher = testDispatcher)

                    VoiceModelManager.getOrFetchModel(mockContext, ioDispatcher = testDispatcher)
                    assertEquals(1, mockedConstruction.constructed().size)
                }
            }
        }
    }

    @Test
    fun verifyModelFetchFromLocalDir() {
        runTest {
            val testDispatcher = StandardTestDispatcher(testScheduler)

            val mockUri = mock<Uri> {
                on { lastPathSegment } doReturn "$modelName.zip"
            }

            val mockedUri = Mockito.mockStatic(Uri::class.java)
            mockedUri.use { mockedUri ->
                mockedUri.`when`<Uri> { Uri.parse(any()) } doReturn mockUri
                Mockito.mockConstruction(Model::class.java).use { mockedConstruction ->
                    // first fetch loads up the model into local dir
                    VoiceModelManager.getOrFetchModel(mockContext, ioDispatcher = testDispatcher)

                    // clean up internal cache of VoiceModelManager
                    val field = VoiceModelManager::class.java.getDeclaredField("modelCache")
                    field.isAccessible = true
                    (field.get(null) as MutableMap<*, *>).clear()

                    VoiceModelManager.getOrFetchModel(mockContext, ioDispatcher = testDispatcher)
                    assertEquals(2, mockedConstruction.constructed().size)

                    (field.get(null) as MutableMap<*, *>).clear()
                }
            }
        }
    }
}