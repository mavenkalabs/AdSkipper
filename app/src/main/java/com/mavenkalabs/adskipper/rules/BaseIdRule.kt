package com.mavenkalabs.adskipper.rules

import android.view.accessibility.AccessibilityNodeInfo
import kotlin.Any

abstract class BaseIdRule(id: String, packageName: String) : BaseRule {
    protected val qualifiedId: String = listOf(packageName, ":id/", id).joinToString(separator = "")

    abstract override fun apply(
        node: AccessibilityNodeInfo,
        parameters: Map<String, Any>?
    ): RuleResult
}
