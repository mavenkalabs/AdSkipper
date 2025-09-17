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
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class AdSkipperService extends AccessibilityService  {
    private long skipAdClickTimestamp = 0;
    private boolean muteAds = false;

    private final AtomicBoolean adInProgress = new AtomicBoolean(false);
    private Timer unmuter = null;

    private final AtomicReference<SharedPreferences.OnSharedPreferenceChangeListener> listenerRef = new AtomicReference<>();

    private static final Map<String, List<String>> PKG_TO_SKIP_ID_MAP = Map.of(
            "com.google.android.youtube", List.of("skip_ad_button", "action"),
            "com.google.android.apps.youtube.music", List.of("skip_ad_button", "snackbar_action")
    );
    private static final Map<String, List<String>> PKG_TO_ADVERT_ID_MAP = Map.of(
            "com.google.android.youtube", List.of("player_learn_more_button", "ad_progress_text"),
            "com.google.android.apps.youtube.music", List.of("player_learn_more_button", "ad_progress_text")
    );

    private static final long QUIET_INTERVAL = 1000;

    private static final String TAG = AdSkipperService.class.getName();


    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        try {
            if (muteAds) {
                checkAndHandleAdEvt(event);
            }
            checkAndHandleSkipEvt(event);
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error", e);
        }
    }

    private void checkAndHandleAdEvt(AccessibilityEvent event) {
        final String eventPkgName = (event.getPackageName() != null ? event.getPackageName().toString() : null);
        final AccessibilityNodeInfo eventSource = event.getSource();
        if (eventSource != null && PKG_TO_ADVERT_ID_MAP.containsKey(eventPkgName)) {
            List<AccessibilityNodeInfo> nodes = Collections.emptyList();
            for (String viewId : Objects.requireNonNull(PKG_TO_ADVERT_ID_MAP.get(eventPkgName))) {
                nodes =
                        eventSource.findAccessibilityNodeInfosByViewId(
                                String.join("", eventPkgName, ":id/", viewId));
                if (!nodes.isEmpty()) break;
            }

            if (!nodes.isEmpty()) {
                if (adInProgress.compareAndSet(false, true)) {
                    toggleMute(true);
                    Log.d(TAG, "checkAndHandleAdEvt: Ad detected");
                }

                updateUnmuter();
            }
        }
    }

    private void checkAndHandleSkipEvt(AccessibilityEvent event) {
        if ((System.currentTimeMillis() - skipAdClickTimestamp) < QUIET_INTERVAL) {
            return;
        }

        final String eventPkgName = (event.getPackageName() != null ? event.getPackageName().toString() : null);
        final AccessibilityNodeInfo eventSource = event.getSource();
        if (eventSource != null && PKG_TO_SKIP_ID_MAP.containsKey(eventPkgName)) {
            List<AccessibilityNodeInfo> nodes = Collections.emptyList();
            for (String viewId : Objects.requireNonNull(PKG_TO_SKIP_ID_MAP.get(eventPkgName))) {
                nodes =
                        eventSource.findAccessibilityNodeInfosByViewId(
                                String.join("", eventPkgName, ":id/", viewId));
                if (!nodes.isEmpty()) break;
            }
            if (!nodes.isEmpty()) {
                nodes.stream()
                        .filter(AccessibilityNodeInfo::isClickable)
                        .filter(AccessibilityNodeInfo::isVisibleToUser)
                        .filter(AccessibilityNodeInfo::isEnabled)
                        .findFirst()
                        .ifPresent(accessibilityNodeInfo -> {
                            accessibilityNodeInfo.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                            if (muteAds) {
                                toggleMute(false);
                            }
                            skipAdClickTimestamp = System.currentTimeMillis();
                            Log.d(TAG, "checkAndHandleSkipEvt: Skipped ad");
                        });
            }
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

    private void updateUnmuter() {
        if (unmuter != null) {
            unmuter.cancel();
        }

        unmuter = new Timer(true);
        unmuter.schedule(new TimerTask() {
                             @Override
                             public void run() {
                                 toggleMute(false);
                                 adInProgress.compareAndSet(true, false);
                                 Log.d(TAG, "Unmuter task executed");
                             }
                         },
                QUIET_INTERVAL);
    }
}
