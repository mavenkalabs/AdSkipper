package com.mavenkalabs.adskipper.service;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Path;
import android.graphics.Rect;
import android.media.AudioManager;
import android.util.Log;
import android.view.ViewConfiguration;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import androidx.preference.PreferenceManager;

import com.mavenkalabs.adskipper.util.LogWriter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

public class AdSkipperService extends AccessibilityService  {
    public static final String MUTE_ADS_PREF = "mute_ads";

    public static final String CAPTURE_LOGS_PREF = "enable_logging";

    private boolean muteAds = false;

    private boolean captureLogs = false;

    private LogWriter logWriter;

    private boolean adInProgress = false;

    private long lastClickTimestamp = 0L;

    private final AtomicReference<SharedPreferences.OnSharedPreferenceChangeListener> listenerRef = new AtomicReference<>();

    private static final Map<String, List<String>> PKG_TO_SKIP_ID_MAP = Map.of(
            "com.google.android.youtube", List.of("skip_ad_button", "modern_miniplayer_skip_ad_button"),
            "com.google.android.apps.youtube.music", List.of("skip_ad_button")
    );
    private static final Map<String, List<String>> PKG_TO_ADVERT_ID_MAP = Map.of(
            "com.google.android.youtube", List.of("player_learn_more_button", "ad_progress_text",
                    "modern_miniplayer_ad_badge", "ad_badge&!collapsible_ad_cta_overlay_container"),
            "com.google.android.apps.youtube.music", List.of("player_learn_more_button", "ad_progress_text")
    );

    private static final String TAG = AdSkipperService.class.getName();

    private static final long QUIET_INTERVAL = 1000;

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        try {
            final String eventPkgName = (event.getPackageName() != null ? event.getPackageName().toString() : null);
            final AccessibilityNodeInfo rootNode = getRootInActiveWindow();
            if (rootNode != null && PKG_TO_ADVERT_ID_MAP.containsKey(eventPkgName)) {
                boolean conditionSatisfied = false;
                for (String viewId : Objects.requireNonNull(PKG_TO_ADVERT_ID_MAP.get(eventPkgName))) {
                    List<String> mustExistViewIds = new ArrayList<>();
                    List<String> mustNotExistViewIds = new ArrayList<>();
                    if (viewId.contains("&")) {
                        String[] viewIdArr = viewId.split("&");
                        Arrays.stream(viewIdArr).forEach(s -> {
                           if (s.startsWith("!")) {
                               mustNotExistViewIds.add(s.substring(1));
                           } else {
                               mustExistViewIds.add(s);
                           }
                        });
                    } else {
                        mustExistViewIds.add(viewId);
                    }

                    conditionSatisfied =  mustExistViewIds.stream().allMatch(s -> {
                        List<AccessibilityNodeInfo> nodes = rootNode.findAccessibilityNodeInfosByViewId(
                                String.join("", eventPkgName, ":id/", s));
                        return (nodes != null && !nodes.isEmpty());
                    });
                    if (conditionSatisfied && !mustNotExistViewIds.isEmpty()) {
                        conditionSatisfied = mustNotExistViewIds.stream().allMatch(s -> {
                            List<AccessibilityNodeInfo> nodes = rootNode.findAccessibilityNodeInfosByViewId(
                                    String.join("", eventPkgName, ":id/", s));
                            return (nodes == null || nodes.isEmpty());
                        });
                    }
                    if (conditionSatisfied) {
                        if (muteAds && !adInProgress) {
                            toggleMute(true);
                            adInProgress = true;
                            Log.d(TAG, "onAccessibilityEvent: Detected ad");
                            if (captureLogs) {
                                if (logWriter == null) {
                                    logWriter = new LogWriter(getApplicationContext());
                                }
                                logWriter.log(getRootInActiveWindow(), LogWriter.EventType.AD);
                            }
                        }
                        break;
                    }
                }

                if (muteAds && !conditionSatisfied) {
                    if (adInProgress) {
                        toggleMute(false);
                        adInProgress = false;
                    }
                }
            }

            if ((System.currentTimeMillis() - lastClickTimestamp) > QUIET_INTERVAL &&
                    rootNode != null && PKG_TO_SKIP_ID_MAP.containsKey(eventPkgName)) {
                for (String viewId : Objects.requireNonNull(PKG_TO_SKIP_ID_MAP.get(eventPkgName))) {
                    List<String> mustExistViewIds = new ArrayList<>();
                    List<String> mustNotExistViewIds = new ArrayList<>();

                    if (viewId.contains("&")) {
                        String[] viewIdArr = viewId.split("&");
                        Arrays.stream(viewIdArr).forEach(s -> {
                            if (s.startsWith("!")) {
                                mustNotExistViewIds.add(s.substring(1));
                            } else {
                                mustExistViewIds.add(s);
                            }
                        });
                    } else {
                        mustExistViewIds.add(viewId);
                    }

                    final List<AccessibilityNodeInfo> foundNodes = new ArrayList<>();
                    boolean conditionSatisfied =  mustExistViewIds.stream().allMatch(s -> {
                        List<AccessibilityNodeInfo> nodes = rootNode.findAccessibilityNodeInfosByViewId(
                                String.join("", eventPkgName, ":id/", s));
                        boolean found = (nodes != null && !nodes.isEmpty());
                        if (found) {
                            foundNodes.addAll(nodes);
                        }
                        return found;
                    });
                    if (conditionSatisfied && !mustNotExistViewIds.isEmpty()) {
                        conditionSatisfied = mustNotExistViewIds.stream().allMatch(s -> {
                            List<AccessibilityNodeInfo> nodes = rootNode.findAccessibilityNodeInfosByViewId(
                                    String.join("", eventPkgName, ":id/", s));
                            return (nodes == null || nodes.isEmpty());
                        });
                    }
                    if (conditionSatisfied && !foundNodes.isEmpty()) {
                        foundNodes.stream()
                                .filter(AccessibilityNodeInfo::isClickable)
                                .findFirst()
                                .ifPresent(accessibilityNodeInfo -> {
                                    tap(accessibilityNodeInfo);
                                    lastClickTimestamp = System.currentTimeMillis();
                                    Log.d(TAG, "onAccessibilityEvent: Skipped ad");
                                    if (captureLogs) {
                                        if (logWriter == null) {
                                            logWriter = new LogWriter(getApplicationContext());
                                        }
                                        logWriter.log(getRootInActiveWindow(), LogWriter.EventType.SKIP);
                                    }
                                });

                        break;
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error", e);
        }
    }

    private void tap(AccessibilityNodeInfo node) {
        Rect nodeBounds = new Rect();
        node.getBoundsInScreen(nodeBounds);
        Path tapPath = new Path();
        tapPath.moveTo(nodeBounds.centerX(), nodeBounds.centerY());
        GestureDescription.StrokeDescription tapStroke =
                new GestureDescription.StrokeDescription(tapPath, 0, ViewConfiguration.getTapTimeout());

        GestureDescription.Builder gestureBuilder = new GestureDescription.Builder();
        gestureBuilder.addStroke(tapStroke);
        dispatchGesture(gestureBuilder.build(), null, null);
    }

    private void toggleMute(boolean mute) {
        AudioManager audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        boolean isCurrentlyMuted = audioManager.isStreamMute(AudioManager.STREAM_MUSIC);
        if (isCurrentlyMuted != mute) {
            audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC,
                    mute ? AudioManager.ADJUST_MUTE : AudioManager.ADJUST_UNMUTE, 0);
        }
        if (mute) {
            Log.d(TAG, "Toggling mute to true");
        } else {
            Log.d(TAG, "Toggling mute to false");
        }
    }

    @Override
    public boolean onUnbind(Intent intent) {
        toggleMute(false);
        return true;
    }

    @Override
    public void onInterrupt() {
        toggleMute(false);
    }

    @Override
    protected void onServiceConnected() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        muteAds = prefs.getBoolean(MUTE_ADS_PREF, false);
        SharedPreferences.OnSharedPreferenceChangeListener listener;
        prefs.registerOnSharedPreferenceChangeListener(listener = (p, key) -> {
            Log.d(TAG, "onServiceConnected: pref changed " + key);

            if (Objects.equals(key, MUTE_ADS_PREF)) {
                muteAds = p.getBoolean(key, false);
                Log.d(TAG, "onServiceConnected: muteAds now " + muteAds);
            } else if (Objects.equals(key, CAPTURE_LOGS_PREF)) {
                captureLogs = p.getBoolean(key, false);
                if (!captureLogs) {
                    if (logWriter != null) {
                        final LogWriter ref = logWriter;
                        logWriter = null;
                        ref.close();
                    }
                }
                Log.d(TAG, "onServiceConnected: captureLogs now " + captureLogs);
            }
        });
        listenerRef.set(listener);
    }
}
