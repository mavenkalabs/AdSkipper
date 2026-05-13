package com.mavenkalabs.adskipper.rules

import android.view.accessibility.AccessibilityNodeInfo

class RuleResult (
    val isPassed: Boolean,
    val filteredNodes: MutableList<AccessibilityNodeInfo>? = null
)
