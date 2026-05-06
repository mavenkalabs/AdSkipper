package com.mavenkalabs.adskipper.rules;

import android.view.accessibility.AccessibilityNodeInfo;

public abstract class BaseIdRule implements BaseRule {
    protected final String qualifiedId;

    public BaseIdRule(String id, String packageName) {
        this.qualifiedId = String.join("", packageName, ":id/", id);
    }

    public abstract RuleResult apply(AccessibilityNodeInfo node);
}
