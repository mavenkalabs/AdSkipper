package com.mavenkalabs.adskipper.service;

import android.accessibilityservice.AccessibilityService;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import java.util.List;

public class AdSkipperService extends AccessibilityService {
    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event.getSource() != null) {
           List<AccessibilityNodeInfo> nodes = event.getSource().findAccessibilityNodeInfosByViewId("com.google.android.youtube:id/skip_ad_button");
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
