package com.mavenkalabs.adskipper.rules;

import android.view.accessibility.AccessibilityNodeInfo;

import java.util.List;

public class MustExistRule extends  BaseIdRule {
    public MustExistRule(String id, String packageName) {
        super(id, packageName);
    }

    @Override
    public RuleResult apply(AccessibilityNodeInfo node) {
        List<AccessibilityNodeInfo> nodes = node.findAccessibilityNodeInfosByViewId(this.qualifiedId);
        return new RuleResult(nodes != null && !nodes.isEmpty(), nodes);
    }
}
