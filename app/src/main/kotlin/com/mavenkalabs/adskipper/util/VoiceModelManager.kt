package com.mavenkalabs.adskipper.util

import android.content.Context
import androidx.core.net.toUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.vosk.Model
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

enum class Language {
    EN_US,
    ES_ES,
}

object VoiceModelManager {
    private val MODEL_URLS = mapOf(
        Language.EN_US to "https://alphacephei.com/vosk/models/vosk-model-small-en-us-0.15.zip",
        Language.ES_ES to "https://alphacephei.com/vosk/models/vosk-model-small-es-0.42.zip"
    )
    /**
     * Checks if the model folder exists locally, or downloads and unzips it.
     * Returns an initialized voice model instance ready for your Voice Controller.
     */
    suspend fun getOrFetchModel(
        context: Context,
        language: Language = Language.EN_US,
        onProgress: (Int) -> Unit = {}
    ): Model? = withContext(Dispatchers.IO) {
        val modelUrl = MODEL_URLS[language]
        val modelDirName = modelUrl?.toUri()?.lastPathSegment?.let { File(it).nameWithoutExtension }
        if (modelDirName == null) {
            return@withContext null
        }

        val targetDirectory = File(context.filesDir, modelDirName)

        if (isModelValid(targetDirectory)) {
            return@withContext Model(targetDirectory.absolutePath)
        }

        targetDirectory.deleteRecursively()

        val success = downloadAndExtract(context.filesDir, modelUrl, onProgress)

        if (success && isModelValid(targetDirectory)) {
            Model(targetDirectory.absolutePath)
        } else {
            null
        }
    }

    private fun isModelValid(directory: File): Boolean {
        return directory.exists() && directory.isDirectory && File(directory, "am/final.mdl").exists()
    }

    private fun downloadAndExtract(
        outputDir: File,
        modelUrl: String,
        onProgress: (Int) -> Unit
    ): Boolean {
        var connection: HttpURLConnection? = null
        try {
            val url = URL(modelUrl)
            connection = url.openConnection() as HttpURLConnection
            connection.connect()

            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                return false
            }

            val fileLength = connection.contentLength
            val inputStream = connection.inputStream

            ZipInputStream(inputStream).use { zis ->
                var zipEntry: ZipEntry? = zis.nextEntry
                val buffer = ByteArray(4096)
                var totalBytesRead = 0L

                while (zipEntry != null) {
                    val newFile = File(outputDir, zipEntry.name)

                    if (zipEntry.isDirectory) {
                        newFile.mkdirs()
                    } else {
                        newFile.parentFile?.mkdirs()

                        FileOutputStream(newFile).use { fos ->
                            var len: Int
                            while (zis.read(buffer).also { len = it } > 0) {
                                fos.write(buffer, 0, len)
                                totalBytesRead += len

                                if (fileLength > 0) {
                                    val progress = ((totalBytesRead * 100) / fileLength).toInt()
                                    onProgress(progress)
                                }
                            }
                        }
                    }
                    zis.closeEntry()
                    zipEntry = zis.nextEntry
                }
            }
            return true
        } catch (_: Exception) {
            return false
        } finally {
            connection?.disconnect()
        }
    }
}
