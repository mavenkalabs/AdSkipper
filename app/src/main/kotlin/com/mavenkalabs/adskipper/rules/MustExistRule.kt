package com.mavenkalabs.adskipper.rules

import android.view.accessibility.AccessibilityNodeInfo

class MustExistRule(id: String, packageName: String) : BaseIdRule(id, packageName) {
    override fun apply(
        node: AccessibilityNodeInfo,
        parameters: Map<String, Any>?
    ): RuleResult {
        val nodes = node.findAccessibilityNodeInfosByViewId(this.qualifiedId)
        return RuleResult(nodes?.isNotEmpty() == true, nodes)
    }

    override fun toString() = id
}
