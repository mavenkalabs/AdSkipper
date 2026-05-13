package com.mavenkalabs.adskipper.rules;

import android.view.accessibility.AccessibilityNodeInfo;

import androidx.annotation.NonNull;

import java.util.List;
import java.util.Map;

public class MustExistRule extends  BaseIdRule {
    public MustExistRule(String id, String packageName) {
        super(id, packageName);
    }

    @Override
    public RuleResult apply(AccessibilityNodeInfo node, Map<String, Object> parameters) {
        List<AccessibilityNodeInfo> nodes = node.findAccessibilityNodeInfosByViewId(this.qualifiedId);
        return new RuleResult(nodes != null && !nodes.isEmpty(), nodes);
    }

    @NonNull
    @Override
    public String toString() {
        return qualifiedId.split("/")[1];
    }
}
