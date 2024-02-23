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
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class AdSkipperService extends AccessibilityService  {
    private long advertTimeStamp = 0;

    private long skipAdClickTimestamp = 0;

    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private boolean muteAds = false;

    private final AtomicReference<SharedPreferences.OnSharedPreferenceChangeListener> listenerRef = new AtomicReference<>();

    private static final Map<String, String> PKG_TO_SKIP_ID_MAP = Map.of(
            "com.google.android.youtube", "skip_ad_button",
            "com.google.android.apps.youtube.music", "skip_ad_button"
    );
    private static final Map<String, List<String>> PKG_TO_ADVERT_ID_MAP = Map.of(
            "com.google.android.youtube", List.of("player_learn_more_button", "ad_progress_text"),
            "com.google.android.apps.youtube.music", List.of("player_learn_more_button", "ad_progress_text")
    );

    private static final long QUIET_INTERVAL = 1000;

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
            List<AccessibilityNodeInfo> nodes = Collections.emptyList();
            for (String viewId : Objects.requireNonNull(PKG_TO_ADVERT_ID_MAP.get(eventPkgName))) {
                nodes =
                        event.getSource().findAccessibilityNodeInfosByViewId(
                                String.join("", eventPkgName, ":id/", viewId));
                if (!nodes.isEmpty()) break;
            }

            if (!nodes.isEmpty()) {
                nodes.stream()
                        .findFirst().ifPresent(node -> {
                            Lock writeLock = lock.writeLock();
                            if (writeLock.tryLock()) {
                                try {
                                    if (advertTimeStamp == 0) {
                                        Log.d(TAG, "checkAndHandleAdEvt: Ad detected and unmuter started");
                                        toggleMute(true);
                                        runUnmuter();
                                    }
                                    advertTimeStamp = System.currentTimeMillis();
                                } finally {
                                    writeLock.unlock();
                                }
                            }
                        });
            }
        }
    }

    private void checkAndHandleSkipEvt(AccessibilityEvent event) {
        if ((System.currentTimeMillis() - skipAdClickTimestamp) < QUIET_INTERVAL) {
            Log.d(TAG, "checkAndHandleSkipEvt: ignored skip event");
            return;
        }

        String eventPkgName = (event.getPackageName() != null ? event.getPackageName().toString() : null);
        if (event.getSource() != null && PKG_TO_SKIP_ID_MAP.containsKey(eventPkgName)) {
            List<AccessibilityNodeInfo> nodes =
                    event.getSource().findAccessibilityNodeInfosByViewId(
                            String.join("", eventPkgName, ":id/", PKG_TO_SKIP_ID_MAP.get(eventPkgName)));
            if (!nodes.isEmpty()) {
                nodes.stream()
                        .filter(AccessibilityNodeInfo::isClickable)
                        .filter(AccessibilityNodeInfo::isVisibleToUser)
                        .filter(AccessibilityNodeInfo::isEnabled)
                        .findFirst()
                        .ifPresent(accessibilityNodeInfo -> {
                            Log.d(TAG, "checkAndHandleSkipEvt: Skipped ad and unmuted");
                            accessibilityNodeInfo.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                            toggleMute(false);
                            skipAdClickTimestamp = System.currentTimeMillis();
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
        advertTimeStamp = 0;
        return true;
    }

    @Override
    public void onInterrupt() {
        toggleMute(false);
        advertTimeStamp = 0;
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

    private void runUnmuter() {
        final Timer timer = new Timer(true);
        timer.schedule(new TimerTask() {
                           @Override
                           public void run() {
                               Log.d(TAG, "Unmuter running");
                               Lock readLock = lock.readLock();
                               long lastOccurrence;
                               try {
                                   readLock.lock();
                                   lastOccurrence = advertTimeStamp;
                               } finally {
                                   readLock.unlock();
                               }

                               if (lastOccurrence > 0) {
                                   long currentTimeStamp = System.currentTimeMillis();
                                   if ((currentTimeStamp - lastOccurrence) >= QUIET_INTERVAL) {
                                       Lock writeLock = lock.writeLock();
                                       if (writeLock.tryLock()) {
                                           try {
                                               Log.d(TAG, "Unmuter is unmuting and then dying");
                                               toggleMute(false);

                                               advertTimeStamp = 0;
                                               timer.cancel();
                                           } finally {
                                            writeLock.unlock();
                                           }
                                       }
                                   }
                               }
                           }
                       },
                QUIET_INTERVAL,
                QUIET_INTERVAL);
    }
}
