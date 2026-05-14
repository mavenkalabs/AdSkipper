package com.mavenkalabs.adskipper.util

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo
import java.io.BufferedWriter
import java.io.OutputStreamWriter
import java.io.Writer
import java.lang.AutoCloseable
import java.nio.charset.StandardCharsets
import java.text.MessageFormat
import java.time.Instant
import java.util.Arrays
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import java.util.stream.Collectors

class AppLog private constructor() : AutoCloseable {
    enum class EventType {
        SKIP, AD
    }

    private val enabledRef = AtomicBoolean(false)
    private val logWriterRef = AtomicReference<BufferedWriter?>()

    override fun close() {
        disable()
    }

    companion object {
        private val TAG: String = AppLog::class.java.getName()

        private val instance = AppLog()
        @JvmStatic
        fun enable(context: Context?) {
            if (context != null) {
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

            instance.enabledRef.set(true)
        }

        @JvmStatic
        fun disable() {
            val logWriter: Writer? = instance.logWriterRef.get()
            if (logWriter != null) {
                try {
                    logWriter.close()
                } catch (e: Throwable) {
                    e(TAG, "Error in closing AppLog output file", e)
                }
            }

            instance.enabledRef.set(false)
        }

        @JvmStatic
        fun logAccessibilityEvent(rootNode: AccessibilityNodeInfo, eventType: EventType) {
            val buffer = StringBuilder()
            buffer.append("Logging event of type ")
                .append(eventType.name)
                .append(System.lineSeparator())
            logNode(rootNode, buffer, "")

            var executor: ExecutorService? = null
            try {
                executor = Executors.newSingleThreadExecutor()
                executor.execute { safelyWriteToLog(TAG, buffer.toString()) }
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
            if (instance.enabledRef.get()) {
                val formattedMessage = MessageFormat.format(message, *args)
                Log.d(tag, formattedMessage)
                safelyWriteToLog(tag, formattedMessage)
            }
        }

        private fun safelyWriteToLog(tag: String, formattedMessage: String?) {
            val logWriter: BufferedWriter? = instance.logWriterRef.get()
            if (logWriter != null) {
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        logWriter.write(
                            listOf(
                                Instant.now().toString(),
                                tag,
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

        @JvmStatic
        fun e(tag: String, message: String, t: Throwable, vararg args: Any?) {
            if (instance.enabledRef.get()) {
                val formattedMessage = MessageFormat.format(message, *args)
                Log.e(tag, formattedMessage, t)
                safelyWriteToLog(tag, formattedMessage)
                safelyWriteToLog(tag, t.message)
                safelyWriteToLog(
                    tag,
                    Arrays.stream<StackTraceElement>(t.stackTrace)
                        .map<String> { it.toString() }
                        .collect(Collectors.joining(System.lineSeparator())))
            }
        }
    }
}
