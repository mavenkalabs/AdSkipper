package com.mavenkalabs.adskipper.rules;

import android.view.accessibility.AccessibilityNodeInfo;

import java.util.List;

public class MustNotExistRule extends BaseIdRule{
    public MustNotExistRule(String id, String packageName) {
        super(id, packageName);
    }

    @Override
    public RuleResult apply(AccessibilityNodeInfo node) {
        List<AccessibilityNodeInfo> nodes = node.findAccessibilityNodeInfosByViewId(this.qualifiedId);
        return new RuleResult(nodes == null || nodes.isEmpty());
    }
}
