package com.mavenkalabs.adskipper.util

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo
import kotlinx.coroutines.runBlocking
import java.io.BufferedWriter
import java.io.OutputStreamWriter
import java.lang.AutoCloseable
import java.nio.charset.StandardCharsets
import java.text.MessageFormat
import java.time.Instant
import java.util.Arrays
import java.util.concurrent.atomic.AtomicBoolean
import java.util.stream.Collectors

class AppLog private constructor() : AutoCloseable {
    enum class EventType {
        SKIP, AD
    }

    private val enabledRef = AtomicBoolean(false)

    private val logBuffer = CircularBuffer<String>(20)

    override fun close() {
        disable()
    }

    companion object {
        private val TAG: String = AppLog::class.java.name

        private val instance = AppLog()
        @JvmStatic
        fun enable() {
            instance.enabledRef.set(true)
        }

        @JvmStatic
        fun disable() {
            instance.enabledRef.set(false)
            runBlocking {
                while (!instance.logBuffer.isEmpty()) {
                    instance.logBuffer.dequeue()
                }
            }
        }


        @JvmStatic
        fun dump(context: Context?) {
            val resolver = context?.contentResolver
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
                    val uri = resolver?.insert(
                        MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                        contentValues
                    )
                    if (uri != null) {
                        BufferedWriter(
                            OutputStreamWriter(
                                resolver.openOutputStream(uri), StandardCharsets.UTF_8
                            )
                        ).use { writer ->
                            while (true) {
                                val item = runBlocking {
                                    instance.logBuffer.dequeue()
                                } ?: break
                                writer.write(item)
                                writer.newLine()
                            }

                            writer.flush()
                        }
                    }
                }
            } catch (t: Throwable) {
                e(TAG, "Error in creating AppLog output file", t)
            }
        }

        @JvmStatic
        fun logAccessibilityEvent(rootNode: AccessibilityNodeInfo, eventType: EventType) {
            if (instance.enabledRef.get()) {
                val buffer = StringBuilder()
                buffer.append("Logging event of type ")
                    .append(eventType.name)
                    .append(System.lineSeparator())
                logNode(rootNode, buffer, "")
                safelyWriteToLog(TAG, buffer.toString())
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
                val childNode = node.getChild(i)
                if (childNode != null) {
                    logNode(childNode, sbuf, prefix)
                }
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
            val logWriter = instance.logBuffer
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    runBlocking {
                        logWriter.enqueue(listOf(
                            Instant.now().toString(),
                            tag,
                            formattedMessage
                        ).joinToString()
                        )
                    }
                }
            } catch (_: Throwable) {
                // ignore
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
