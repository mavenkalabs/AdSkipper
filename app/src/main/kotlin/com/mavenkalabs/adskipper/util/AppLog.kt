package com.mavenkalabs.adskipper.util

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo
import com.mavenkalabs.adskipper.BuildConfig
import java.io.BufferedWriter
import java.io.OutputStreamWriter
import java.lang.AutoCloseable
import java.nio.charset.StandardCharsets
import java.text.MessageFormat
import java.time.Instant
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicReference

class AppLog private constructor() : AutoCloseable {
    private val logWriterRef = AtomicReference<BufferedWriter?>()

    override fun close() {
        disableA11yLogging()
    }

    companion object {
        private val TAG: String = AppLog::class.java.getName()

        private val instance = AppLog()
        fun enableA11yLogging(context: Context) {
            val resolver = context.contentResolver
            val contentValues = ContentValues()
            contentValues.put(
                MediaStore.MediaColumns.DISPLAY_NAME,
                "adskipper-debug-" + System.currentTimeMillis() + ".txt"
            )
            contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "text/plain")
            contentValues.put(
                MediaStore.MediaColumns.RELATIVE_PATH,
                Environment.DIRECTORY_DOWNLOADS
            )

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val uri = resolver.insert(
                        MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                        contentValues
                    )
                    if (uri != null) {
                        val logWriter = BufferedWriter(
                            OutputStreamWriter(
                                resolver.openOutputStream(uri), StandardCharsets.UTF_8
                            )
                        )
                        instance.logWriterRef.set(logWriter)
                    }
                }
            } catch (t: Throwable) {
                e(TAG, "Error in creating AppLog output file", t)
            }
        }

        fun disableA11yLogging() {
            val logWriter = instance.logWriterRef.get()
            if (logWriter != null) {
                try {
                    logWriter.close()
                } catch (e: Throwable) {
                    e(TAG, "Error in closing AppLog output file", e)
                }
            }
        }

        fun logNodeTree(rootNode: AccessibilityNodeInfo) {
            val buffer = StringBuilder()
            buffer.append("Logging accessibility node tree")
                .append(System.lineSeparator())
            logNode(rootNode, buffer, "")

            var executor: ExecutorService? = null
            try {
                executor = Executors.newSingleThreadExecutor()
                executor.execute { safelyWriteToLog(buffer.toString()) }
            } finally {
                executor?.close()
            }
        }

        private fun logNode(node: AccessibilityNodeInfo, sbuf: StringBuilder, prefix: String) {
            var prefix = prefix
            if (node.viewIdResourceName != null) {
                sbuf.append(prefix).append(node).append(System.lineSeparator())
                prefix = "$prefix "
            }

            val childCount = node.childCount
            for (i in 0..<childCount) {
                logNode(node.getChild(i), sbuf, prefix)
            }
        }


        fun d(tag: String, message: String, vararg args: Any?) {
            if (BuildConfig.DEBUG) {
                val formattedMessage = MessageFormat.format(message, *args)
                Log.d(tag, formattedMessage)
            }
        }

        private fun safelyWriteToLog(formattedMessage: String?) {
            val logWriter: BufferedWriter? = instance.logWriterRef.get()
            if (logWriter != null) {
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        logWriter.write(
                            listOf(
                                Instant.now().toString(),
                                TAG,
                                formattedMessage
                            ).joinToString()
                        )
                        logWriter.newLine()
                        logWriter.flush()
                    }
                } catch (_: Throwable) {
                    // ignore
                }
            }
        }

        fun e(tag: String, message: String, t: Throwable, vararg args: Any?) {
            if (BuildConfig.DEBUG) {
                val formattedMessage = MessageFormat.format(message, *args)
                Log.e(tag, formattedMessage, t)
            }
        }
    }
}
