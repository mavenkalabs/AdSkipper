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

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

public class AdSkipperService extends AccessibilityService  {
    private final AtomicLong advertTimeStamp = new AtomicLong(0);

    private boolean muteAds = false;

    private final AtomicReference<SharedPreferences.OnSharedPreferenceChangeListener> listenerRef = new AtomicReference<>();

    private static final Map<String, String> PKG_TO_SKIP_ID_MAP = Map.of(
            "com.google.android.youtube", "skip_ad_button",
            "com.google.android.apps.youtube.music", "skip_ad_button"
    );
    private static final Map<String, String> PKG_TO_ADVERT_ID_MAP = Map.of(
            "com.google.android.youtube", "ad_progress_text",
            "com.google.android.apps.youtube.music", "ad_progress_text"
    );

    private static final long UNMUTER_RUN_INTERVAL = 1000;

    private static final String TAG = AdSkipperService.class.getName();

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (muteAds) {
            checkAndHandleAdEvt(event);
        }
        checkAndHandleSkipEvt(event);
    }

    private void checkAndHandleAdEvt(AccessibilityEvent event) {
        String eventPkgName = (event.getPackageName() != null ? event.getPackageName().toString() : null);
        if (event.getSource() != null && PKG_TO_ADVERT_ID_MAP.containsKey(eventPkgName)) {
            List<AccessibilityNodeInfo> nodes =
                    event.getSource().findAccessibilityNodeInfosByViewId(
                            String.join("", eventPkgName, ":id/", PKG_TO_ADVERT_ID_MAP.get(eventPkgName)));
            if (!nodes.isEmpty()) {
                Log.i(TAG, "checkAndHandleAdEvt: Ad detected");
                nodes.stream()
                        .findFirst().ifPresent(node -> {
                            if (advertTimeStamp.getAndSet(System.currentTimeMillis())  == 0) {
                                toggleMute(true);
                                runUnmuter();
                            }
                        });
            }
        }
    }

    private void checkAndHandleSkipEvt(AccessibilityEvent event) {
        String eventPkgName = (event.getPackageName() != null ? event.getPackageName().toString() : null);
        if (event.getSource() != null && PKG_TO_SKIP_ID_MAP.containsKey(eventPkgName)) {
            List<AccessibilityNodeInfo> nodes =
                    event.getSource().findAccessibilityNodeInfosByViewId(
                            String.join("", eventPkgName, ":id/", PKG_TO_SKIP_ID_MAP.get(eventPkgName)));
            if (!nodes.isEmpty()) {
                Log.i(TAG, "checkAndHandleSkipEvt: Skipped ad");
                nodes.stream()
                        .filter(AccessibilityNodeInfo::isClickable)
                        .findFirst()
                        .ifPresent(accessibilityNodeInfo -> {
                            accessibilityNodeInfo.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                            toggleMute(false);
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
    }

    @Override
    public boolean onUnbind(Intent intent) {
        toggleMute(false);
        advertTimeStamp.set(0);
        return true;
    }

    @Override
    public void onInterrupt() {
        toggleMute(false);
        advertTimeStamp.set(0);
    }

    @Override
    protected void onServiceConnected() {
        SharedPreferences prefs = getApplicationContext().getSharedPreferences(
                getApplicationContext().getPackageName() + "_preferences",
                Context.MODE_PRIVATE);
        muteAds = prefs.getBoolean(ServiceEnabledFragment.MUTE_ADS_PREF, false);
        SharedPreferences.OnSharedPreferenceChangeListener listener;
        prefs.registerOnSharedPreferenceChangeListener(listener = (p, key) -> {
            Log.i(TAG, "onServiceConnected: pref changed " + key);

            if (Objects.equals(key, ServiceEnabledFragment.MUTE_ADS_PREF)) {
                muteAds = p.getBoolean(key, false);
                Log.i(TAG, "onServiceConnected: muteAds now " + muteAds);
            }
        });
        listenerRef.set(listener);
    }

    private void runUnmuter() {
        final Timer timer = new Timer(true);
        timer.schedule(new TimerTask() {
                           @Override
                           public void run() {
                               Log.i(TAG, "Unmuter running");
                               long lastOccurrence = advertTimeStamp.get();
                               if (lastOccurrence > 0) {
                                   long currentTimeStamp = System.currentTimeMillis();
                                   if ((currentTimeStamp - lastOccurrence) >= UNMUTER_RUN_INTERVAL) {
                                       Log.i(TAG, "Unmuter is unmuting and then dying");
                                       toggleMute(false);
                                       advertTimeStamp.set(0);
                                       timer.cancel();
                                   }
                               }
                           }
                       },
                UNMUTER_RUN_INTERVAL,
                UNMUTER_RUN_INTERVAL);
    }
}
