package com.mavenkalabs.adskipper.rules;

import android.view.accessibility.AccessibilityNodeInfo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MultiRule implements BaseRule {
    private final Conditionality conditionality;
    private final List<BaseRule> childRules;

    public MultiRule(Conditionality c, BaseRule... rules) {
        this.conditionality = c;
        this.childRules = Arrays.asList(rules);
    }

    @Override
    public RuleResult apply(AccessibilityNodeInfo node) {
        if (conditionality == Conditionality.AND) {
            List<AccessibilityNodeInfo> allFilteredNodes = new ArrayList<>();
            boolean passed = childRules.stream().allMatch(childRule -> {
                RuleResult result = childRule.apply(node);
                List<AccessibilityNodeInfo> resultNodes = result.getFilteredNodes();
                if (resultNodes != null) {
                    allFilteredNodes.addAll(resultNodes);
                }
                return result.isPassed();
            });

            return new RuleResult(passed, passed ? allFilteredNodes : null);
        } else {
            List<AccessibilityNodeInfo> allFilteredNodes = new ArrayList<>();
            boolean passed = childRules.stream().anyMatch(childRule -> {
                RuleResult result = childRule.apply(node);
                List<AccessibilityNodeInfo> resultNodes = result.getFilteredNodes();
                if (result.isPassed() && resultNodes != null) {
                    allFilteredNodes.addAll(resultNodes);
                }
                return result.isPassed();
            });
            return new RuleResult(passed, passed ? allFilteredNodes : null);
        }
    }


    public enum Conditionality {
        AND, OR
    }
}
