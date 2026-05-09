package com.mavenkalabs.adskipper.rules;

import android.view.accessibility.AccessibilityNodeInfo;

import androidx.annotation.NonNull;

import java.util.List;
import java.util.Map;

public class MustNotExistRule extends BaseIdRule{
    public MustNotExistRule(String id, String packageName) {
        super(id, packageName);
    }

    @Override
    public RuleResult apply(AccessibilityNodeInfo node, Map<String, Object> parameters) {
        List<AccessibilityNodeInfo> nodes = node.findAccessibilityNodeInfosByViewId(this.qualifiedId);
        return new RuleResult(nodes == null || nodes.isEmpty());
    }

    @NonNull
    @Override
    public String toString() {
        return String.join("", "!", qualifiedId.split("/")[1]);
    }
}
