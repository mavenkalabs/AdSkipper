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

import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LogWriter implements Closeable {
    private static final String TAG = LogWriter.class.getName();

    public enum EventType { SKIP, AD }

    private OutputStream out;
    public LogWriter(Context context )  {
        ContentResolver resolver = context.getContentResolver();
        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, "adskipper-debug-" + System.currentTimeMillis() + ".txt");
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "text/plain");
        contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                Uri uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues);
                if (uri != null) {
                    out = resolver.openOutputStream(uri);
                }
            }
        } catch (Throwable t) {
            Log.e(TAG, "Error in creating LogWriter", t);
        }
    }

    public void log(AccessibilityNodeInfo rootNode, EventType eventType) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            StringBuilder buffer = new StringBuilder();
            buffer.append(LocalDateTime.now().toString())
                    .append(",")
                    .append(eventType.name())
                    .append(System.lineSeparator());
            logNode(rootNode, buffer, "");

            try (ExecutorService executor = Executors.newSingleThreadExecutor()) {
                executor.execute(() -> {
                    try {
                        out.write(buffer.toString().getBytes(StandardCharsets.UTF_8));
                    } catch (IOException e) {
                        Log.e(TAG, "Error writing to log", e);
                    }
                });
            }
        }
    }

    private void logNode(AccessibilityNodeInfo node, StringBuilder sbuf, String prefix) {
        if (node.getViewIdResourceName() != null) {
            sbuf.append(prefix).append(node).append(System.lineSeparator());
            prefix = prefix.concat(" ");
        }

        int childCount = node.getChildCount();
        for (int i = 0 ; i < childCount ; i++) {
            logNode(node.getChild(i), sbuf, prefix);
        }
    }


    @Override
    public void close() {
        if (out != null) {
            try {
                out.close();
            } catch (IOException e) {
                Log.e(TAG, "Error in closing LogWriter", e);
            }
        }
    }
}
