package com.mavenkalabs.adskipper.rules;

import android.view.accessibility.AccessibilityNodeInfo;

public interface BaseRule {
    RuleResult apply(AccessibilityNodeInfo node);
}
