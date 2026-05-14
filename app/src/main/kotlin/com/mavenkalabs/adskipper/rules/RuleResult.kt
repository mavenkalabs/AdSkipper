package com.mavenkalabs.adskipper.rules

import android.view.accessibility.AccessibilityNodeInfo

data class RuleResult(val isPassed: Boolean = false, val filteredNodes : List<AccessibilityNodeInfo>? = null)
