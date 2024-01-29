package com.mavenkalabs.adskipper.service;

import android.accessibilityservice.AccessibilityService;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import java.util.List;
import java.util.Map;

public class AdSkipperService extends AccessibilityService {
    private static final Map<String, String> PKG_TO_ID_MAP = Map.of(
            "com.google.android.youtube", "skip_ad_button",
            "com.google.android.apps.youtube.music", "skip_ad_button"
    );

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event.getSource() != null && PKG_TO_ID_MAP.containsKey(event.getPackageName().toString())) {
            String id = PKG_TO_ID_MAP.get(event.getPackageName().toString());
            List<AccessibilityNodeInfo> nodes =
                    event.getSource().findAccessibilityNodeInfosByViewId(
                            String.join("", event.getPackageName().toString(), ":id/", id));
           if (!nodes.isEmpty()) {
               nodes.stream()
                       .filter(AccessibilityNodeInfo::isClickable)
                       .findFirst().ifPresent(node -> node.performAction(AccessibilityNodeInfo.ACTION_CLICK));
           }
        }
    }

    @Override
    public void onInterrupt() {

    }
}
