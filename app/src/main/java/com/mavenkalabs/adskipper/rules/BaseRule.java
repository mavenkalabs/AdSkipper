package com.mavenkalabs.adskipper.rules;

import android.view.accessibility.AccessibilityNodeInfo;

import java.util.Map;

public interface BaseRule {
    RuleResult apply(AccessibilityNodeInfo node, Map<String, Object> parameters);
}
