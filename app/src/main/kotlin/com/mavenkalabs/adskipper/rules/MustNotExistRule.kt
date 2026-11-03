package com.mavenkalabs.adskipper.rules

import android.view.accessibility.AccessibilityNodeInfo
import kotlin.Any

class MustNotExistRule(id: String, packageName: String) : BaseIdRule(id, packageName) {
    override fun apply(
        node: AccessibilityNodeInfo,
        parameters: Map<String, Any>?
    ): RuleResult {
        var nodes = node.findAccessibilityNodeInfosByViewId(this.qualifiedId)
        if (index != null) {
            nodes = nodes.filterIndexed { i, _ ->  i == index}
        }
        return RuleResult(nodes == null || nodes.isEmpty())
    }

    override fun toString() = "!$id"
}
