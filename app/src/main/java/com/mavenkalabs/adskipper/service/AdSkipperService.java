package com.mavenkalabs.adskipper.service;

import android.accessibilityservice.AccessibilityService;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import com.mavenkalabs.adskipper.ServiceEnabledFragment;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

public class AdSkipperService extends AccessibilityService  {
    private boolean muteAds = false;

    private boolean adInProgress = false;

    private long lastClickTimestamp = 0L;

    private final AtomicReference<SharedPreferences.OnSharedPreferenceChangeListener> listenerRef = new AtomicReference<>();

    private static final Map<String, List<String>> PKG_TO_SKIP_ID_MAP = Map.of(
            "com.google.android.youtube", List.of("skip_ad_button", "action", "modern_miniplayer_skip_ad_button"),
            "com.google.android.apps.youtube.music", List.of("skip_ad_button", "snackbar_action")
    );
    private static final Map<String, List<String>> PKG_TO_ADVERT_ID_MAP = Map.of(
            "com.google.android.youtube", List.of("player_learn_more_button", "ad_progress_text", "modern_miniplayer_ad_badge"),
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
                List<AccessibilityNodeInfo> foundNodes = Collections.emptyList();
                for (String viewId : Objects.requireNonNull(PKG_TO_ADVERT_ID_MAP.get(eventPkgName))) {
                    foundNodes =
                            rootNode.findAccessibilityNodeInfosByViewId(
                                    String.join("", eventPkgName, ":id/", viewId));
                    if (foundNodes != null && !foundNodes.isEmpty()) {
                        if (muteAds && !adInProgress) {
                            toggleMute(true);
                            adInProgress = true;
                            Log.d(TAG, "onAccessibilityEvent: Detected ad");
                        }
                        break;
                    }
                }

                if (muteAds && (foundNodes == null || foundNodes.isEmpty())) {
                    if (adInProgress) {
                        toggleMute(false);
                        adInProgress = false;
                    }
                }
            }

            if ((System.currentTimeMillis() - lastClickTimestamp) > QUIET_INTERVAL &&
                    rootNode != null && PKG_TO_SKIP_ID_MAP.containsKey(eventPkgName)) {
                for (String viewId : Objects.requireNonNull(PKG_TO_SKIP_ID_MAP.get(eventPkgName))) {
                    List<AccessibilityNodeInfo> foundNodes =
                            rootNode.findAccessibilityNodeInfosByViewId(
                                    String.join("", eventPkgName, ":id/", viewId));
                    if (foundNodes != null && !foundNodes.isEmpty()) {
                        foundNodes.stream()
                                .filter(AccessibilityNodeInfo::isClickable)
                                .filter(AccessibilityNodeInfo::isVisibleToUser)
                                .filter(AccessibilityNodeInfo::isEnabled)
                                .findFirst()
                                .ifPresent(accessibilityNodeInfo -> {
                                    accessibilityNodeInfo.performAction(AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS);
                                    accessibilityNodeInfo.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                                    lastClickTimestamp = System.currentTimeMillis();
                                    Log.d(TAG, "onAccessibilityEvent: Skipped ad");
                                });

                        break;
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error", e);
        }
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
        SharedPreferences prefs = getApplicationContext().getSharedPreferences(
                getApplicationContext().getPackageName() + "_preferences",
                Context.MODE_PRIVATE);
        muteAds = prefs.getBoolean(ServiceEnabledFragment.MUTE_ADS_PREF, false);
        SharedPreferences.OnSharedPreferenceChangeListener listener;
        prefs.registerOnSharedPreferenceChangeListener(listener = (p, key) -> {
            Log.d(TAG, "onServiceConnected: pref changed " + key);

            if (Objects.equals(key, ServiceEnabledFragment.MUTE_ADS_PREF)) {
                muteAds = p.getBoolean(key, false);
                Log.d(TAG, "onServiceConnected: muteAds now " + muteAds);
            }
        });
        listenerRef.set(listener);
    }
}
