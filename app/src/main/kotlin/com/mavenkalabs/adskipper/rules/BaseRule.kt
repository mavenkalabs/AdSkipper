package com.mavenkalabs.adskipper.rules

import android.view.accessibility.AccessibilityNodeInfo

interface BaseRule {
    fun apply(node: AccessibilityNodeInfo, parameters: Map<String, Any>? = null): RuleResult
}
