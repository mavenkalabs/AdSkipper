package com.mavenkalabs.adskipper.rules

import android.view.accessibility.AccessibilityNodeInfo
import java.util.function.Predicate
import java.util.stream.Collectors

class MultiRule(private val conditionality: Conditionality, vararg rules: BaseRule) : BaseRule {
    private val childRules = listOf(*rules)

    override fun apply(
        node: AccessibilityNodeInfo,
        parameters: Map<String, Any>?
    ): RuleResult {
        val allFilteredNodes: MutableList<AccessibilityNodeInfo> = mutableListOf()
        fun ruleCheck(): Predicate<in BaseRule> = { childRule ->
            val result = childRule.apply(node, parameters)
            if (result.isPassed && result.filteredNodes?.isNotEmpty() == true) {
                allFilteredNodes.addAll(result.filteredNodes)
            }
            result.isPassed
        }

        val passed = if (conditionality == Conditionality.AND) {
            childRules.stream().allMatch(ruleCheck())
        } else {
            childRules.stream().anyMatch(ruleCheck())
        }

        return RuleResult(passed, if (passed) allFilteredNodes else null)
    }

    override fun toString(): String {
        return childRules.stream()
                .map { it.toString() }
                .collect(Collectors.joining(if (conditionality == Conditionality.OR) "|" else "&"))
    }

    enum class Conditionality {
        AND, OR
    }
}
