package com.mavenkalabs.adskipper.rules;

import android.view.accessibility.AccessibilityNodeInfo;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class MultiRule implements BaseRule {
    private final Conditionality conditionality;
    private final List<BaseRule> childRules;

    public MultiRule(Conditionality c, BaseRule... rules) {
        this.conditionality = c;
        this.childRules = Arrays.asList(rules);
    }

    @Override
    public RuleResult apply(AccessibilityNodeInfo node, Map<String, Object> parameters) {
        if (conditionality == Conditionality.AND) {
            List<AccessibilityNodeInfo> allFilteredNodes = new ArrayList<>();
            boolean passed = childRules.stream().allMatch(childRule -> {
                RuleResult result = childRule.apply(node, parameters);
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
                RuleResult result = childRule.apply(node, parameters);
                List<AccessibilityNodeInfo> resultNodes = result.getFilteredNodes();
                if (result.isPassed() && resultNodes != null) {
                    allFilteredNodes.addAll(resultNodes);
                }
                return result.isPassed();
            });
            return new RuleResult(passed, passed ? allFilteredNodes : null);
        }
    }

    @NonNull
    @Override
    public String toString() {
        return String.join(conditionality == Conditionality.OR ? "|" : "&", childRules.stream().map(Object::toString).toArray(String[]::new));
    }

    public enum Conditionality {
        AND, OR
    }
}
