package com.mavenkalabs.adskipper.rules

import android.view.accessibility.AccessibilityNodeInfo

class NoRecentUserClickRule(val ruleAsString: String) : BaseRule {
    private val interval: Long
        get() = this.ruleAsString.split(",".toRegex()).last().toLong()

    override fun apply(
        node: AccessibilityNodeInfo,
        parameters: Map<String, Any>?
    ): RuleResult {
        if (parameters != null) {
            val value1 = parameters[RULE_PARAM_LAST_USER_CLICK_TS]
            if (value1 is Long) {
                val clickTS = value1
                if (System.currentTimeMillis() > (clickTS + interval)) {
                    return RuleResult(true)
                }
            }
        }
        return RuleResult(false)
    }

    override fun toString() = "${RULE_ID_NO_RECENT_USER_CLICK},$interval"
}
