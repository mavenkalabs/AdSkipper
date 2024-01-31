package com.mavenkalabs.adskipper.service;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.media.AudioManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import java.util.List;
import java.util.Map;

public class AdSkipperService extends AccessibilityService {
    private boolean audioMuted = false;

    private static final Map<String, String> PKG_TO_SKIP_ID_MAP = Map.of(
            "com.google.android.youtube", "skip_ad_button",
            "com.google.android.apps.youtube.music", "skip_ad_button"
    );
    private static final Map<String, String> PKG_TO_AD_START_ID_MAP = Map.of(
            "com.google.android.youtube", "ad_progress_text",
            "com.google.android.apps.youtube.music", "ad_progress_text"
    );

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        String eventPkgName = event.getPackageName().toString();
        if (event.getSource() != null && PKG_TO_SKIP_ID_MAP.containsKey(eventPkgName)) {
            String id = (audioMuted ? PKG_TO_SKIP_ID_MAP : PKG_TO_AD_START_ID_MAP).get(eventPkgName);
            List<AccessibilityNodeInfo> nodes =
                    event.getSource().findAccessibilityNodeInfosByViewId(
                            String.join("", eventPkgName, ":id/", id));
            if (!nodes.isEmpty()) {
               if (audioMuted) {
                   nodes.stream()
                           .filter(AccessibilityNodeInfo::isClickable)
                           .findFirst().ifPresent(node -> node.performAction(AccessibilityNodeInfo.ACTION_CLICK));
               }

               toggleVolume();
           }
        }
    }

    private void toggleVolume() {
        ((AudioManager)getSystemService(AUDIO_SERVICE)).adjustStreamVolume(AudioManager.STREAM_MUSIC,
                audioMuted ? AudioManager.ADJUST_UNMUTE : AudioManager.ADJUST_MUTE,
                0);
        audioMuted = !audioMuted;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        if (audioMuted) toggleVolume();

        return true;
    }

    @Override
    public void onInterrupt() {
        if (audioMuted) toggleVolume();
    }
}
