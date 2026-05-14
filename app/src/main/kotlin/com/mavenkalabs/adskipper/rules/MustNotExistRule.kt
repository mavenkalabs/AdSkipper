package com.mavenkalabs.adskipper.rules

import android.view.accessibility.AccessibilityNodeInfo
import kotlin.Any

class MustNotExistRule(id: String, packageName: String) : BaseIdRule(id, packageName) {
    override fun apply(
        node: AccessibilityNodeInfo,
        parameters: Map<String, Any>?
    ): RuleResult {
        val nodes = node.findAccessibilityNodeInfosByViewId(this.qualifiedId)
        return RuleResult(nodes == null || nodes.isEmpty())
    }

    override fun toString() = "!$id"
}
