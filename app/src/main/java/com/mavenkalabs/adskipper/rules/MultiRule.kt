package com.mavenkalabs.adskipper.rules

import android.view.accessibility.AccessibilityNodeInfo
import java.util.stream.Collectors

class MultiRule(private val conditionality: Conditionality, vararg rules: BaseRule) : BaseRule {
    private val childRules: List<BaseRule> = listOf(*rules)

    override fun apply(
        node: AccessibilityNodeInfo,
        parameters: Map<String, Any>?
    ): RuleResult {
        if (conditionality == Conditionality.AND) {
            val allFilteredNodes: MutableList<AccessibilityNodeInfo> =
                mutableListOf()
            val passed = childRules.stream().allMatch { childRule ->
                val result = childRule.apply(node, parameters)
                val resultNodes = result.filteredNodes
                if (resultNodes != null) {
                    allFilteredNodes.addAll(resultNodes)
                }
                result.isPassed
            }

            return RuleResult(passed, if (passed) allFilteredNodes else null)
        } else {
            val allFilteredNodes: MutableList<AccessibilityNodeInfo> =
                mutableListOf()
            val passed = childRules.stream().anyMatch { childRule ->
                val result = childRule.apply(node, parameters)
                val resultNodes = result.filteredNodes
                if (result.isPassed && resultNodes != null) {
                    allFilteredNodes.addAll(resultNodes)
                }
                result.isPassed
            }
            return RuleResult(passed, if (passed) allFilteredNodes else null)
        }
    }

    override fun toString(): String {
        return childRules.stream()
                .map<String> { it.toString() }
                .collect(Collectors.joining(if (conditionality == Conditionality.OR) "|" else "&"))
    }

    enum class Conditionality {
        AND, OR
    }
}
