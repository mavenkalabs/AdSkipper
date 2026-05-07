package com.mavenkalabs.adskipper.util;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.accessibility.AccessibilityNodeInfo;

import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.time.Instant;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public class AppLog implements AutoCloseable{
    public enum EventType { SKIP, AD }

    private static final String TAG = AppLog.class.getName();

    private static final AppLog instance = new AppLog();
    private final AtomicBoolean enabledRef = new AtomicBoolean(false);
    private final AtomicReference<BufferedWriter> logWriterRef = new AtomicReference<>();

    private AppLog() {
    }

    @Override
    public void close() {
        disable();
    }

    public static void enable(Context context) {
        if (context != null) {
            ContentResolver resolver = context.getContentResolver();
            ContentValues contentValues = new ContentValues();
            contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, "adskipper-debug-" + System.currentTimeMillis() + ".txt");
            contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "text/plain");
            contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    Uri uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues);
                    if (uri != null) {
                        final BufferedWriter logWriter = new BufferedWriter(new OutputStreamWriter(
                                resolver.openOutputStream(uri), StandardCharsets.UTF_8));
                        instance.logWriterRef.set(logWriter);
                    }
                }
            } catch (Throwable t) {
                AppLog.e(TAG, "Error in creating AppLog output file", t);
            }
        }

        instance.enabledRef.set(true);
    }

    public static void disable() {
        final Writer logWriter = instance.logWriterRef.get();
        if (logWriter != null) {
            try {
                logWriter.close();
            } catch (Throwable e) {
                AppLog.e(TAG, "Error in closing AppLog output file", e);
            }
        }

        instance.enabledRef.set(false);
    }

    public static void logAccessibilityEvent(AccessibilityNodeInfo rootNode, EventType eventType) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && rootNode != null) {
            StringBuilder buffer = new StringBuilder();
            buffer.append("Logging event of type ")
                    .append(eventType.name())
                    .append(System.lineSeparator());
            logNode(rootNode, buffer, "");

            try (ExecutorService executor = Executors.newSingleThreadExecutor()) {
                executor.execute(() -> safelyWriteToLog(TAG, buffer.toString()));
            }
        }
    }

    private static void logNode(AccessibilityNodeInfo node, StringBuilder sbuf, String prefix) {
        if (node != null) {
            if (node.getViewIdResourceName() != null) {
                sbuf.append(prefix).append(node).append(System.lineSeparator());
                prefix = prefix.concat(" ");
            }

            int childCount = node.getChildCount();
            for (int i = 0; i < childCount; i++) {
                logNode(node.getChild(i), sbuf, prefix);
            }
        }
    }


    public static void d(String tag, String message, Object... args) {
        if (instance.enabledRef.get()) {
            String formattedMessage = MessageFormat.format(message, args);
            Log.d(tag, formattedMessage);
            safelyWriteToLog(tag, formattedMessage);
        }
    }

    private static void safelyWriteToLog(String tag, String formattedMessage) {
        final BufferedWriter logWriter = instance.logWriterRef.get();
        if (logWriter != null) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    logWriter.write(String.join(",", Instant.now().toString(), tag, formattedMessage));
                    logWriter.newLine();
                    logWriter.flush();
                }
            } catch (Throwable t) {
                // ignore
            }
        }
    }

    public static void e(String tag, String message, Throwable t, Object... args) {
        if (instance.enabledRef.get()) {
            String formattedMessage = MessageFormat.format(message, args);
            Log.e(tag, formattedMessage, t);
            safelyWriteToLog(tag, formattedMessage);
            safelyWriteToLog(tag, t.getMessage());
            safelyWriteToLog(tag, Arrays.stream(t.getStackTrace()).map(StackTraceElement::toString)
                    .collect(Collectors.joining(System.lineSeparator())));
        }
    }
}
