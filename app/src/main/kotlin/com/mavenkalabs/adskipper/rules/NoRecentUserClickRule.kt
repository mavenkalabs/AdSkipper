package com.mavenkalabs.adskipper.rules

import android.view.accessibility.AccessibilityNodeInfo

class NoRecentUserClickRule(ruleAsString: String) : BaseRule {
    private val interval: Long

    init {
        val arr: Array<String?> =
            ruleAsString.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        require(arr.size == 2)
        interval = arr[1]!!.toLong()
    }

    override fun apply(
        node: AccessibilityNodeInfo,
        parameters: Map<String, Any>?
    ): RuleResult {
        if (parameters != null) {
            val value1 = parameters.get(RuleConstants.Companion.RULE_PARAM_LAST_USER_CLICK_TS)
            if (value1 is Long) {
                val clickTS = value1
                if (System.currentTimeMillis() > (clickTS + interval)) {
                    return RuleResult(true)
                }
            }
        }
        return RuleResult(false)
    }

    override fun toString(): String {
        return listOf(
            RuleConstants.Companion.RULE_ID_NO_RECENT_USER_CLICK,
            interval.toString()).joinToString(",")
    }
}
