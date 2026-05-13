package com.mavenkalabs.adskipper.rules;

import android.view.accessibility.AccessibilityNodeInfo;

import androidx.annotation.NonNull;

import java.util.Map;

import static com.mavenkalabs.adskipper.rules.RuleConstants.RULE_ID_NO_RECENT_USER_CLICK;
import static com.mavenkalabs.adskipper.rules.RuleConstants.RULE_PARAM_LAST_USER_CLICK_TS;

public class NoRecentUserClickRule implements BaseRule {
    private final long interval;

    public NoRecentUserClickRule(String ruleAsString) {
        String[] arr = ruleAsString.split(",");
        if (arr.length != 2) {
            throw new IllegalArgumentException();
        } else {
            interval = Long.parseLong(arr[1]);
        }
    }

    @Override
    public RuleResult apply(AccessibilityNodeInfo node, Map<String, Object> parameters) {
        if (parameters != null) {
            Object value1 = parameters.get(RULE_PARAM_LAST_USER_CLICK_TS);
            if (value1 instanceof Long) {
                long clickTS = (Long) value1;
                if (System.currentTimeMillis() > (clickTS + interval)) {
                    return new RuleResult(true);
                }
            }
        }
        return new RuleResult(false);
    }

    @NonNull
    @Override
    public String toString() {
        return String.join(",", RULE_ID_NO_RECENT_USER_CLICK, String.valueOf(interval)) ;
    }
}
