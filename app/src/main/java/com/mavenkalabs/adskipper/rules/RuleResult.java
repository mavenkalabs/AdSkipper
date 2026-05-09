package com.mavenkalabs.adskipper.rules;

import android.view.accessibility.AccessibilityNodeInfo;

import java.util.Collections;
import java.util.List;

public class RuleResult {
    private final boolean passed;
    private final List<AccessibilityNodeInfo> filteredNodes;

    public RuleResult(boolean passed, List<AccessibilityNodeInfo> filteredNodes) {
        this.passed = passed;
        this.filteredNodes = filteredNodes;
    }

    public RuleResult(boolean passed) {
        this(passed, null);
    }

    public List<AccessibilityNodeInfo> getFilteredNodes() {
        return (filteredNodes == null ? null : Collections.unmodifiableList(filteredNodes));
    }

    public boolean isPassed() {
        return passed;
    }
}
