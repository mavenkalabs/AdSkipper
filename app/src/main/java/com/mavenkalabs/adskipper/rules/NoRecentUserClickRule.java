package com.mavenkalabs.adskipper.rules;

import android.view.accessibility.AccessibilityNodeInfo;

import androidx.annotation.NonNull;

import java.util.Map;

import static com.mavenkalabs.adskipper.rules.RuleConstants.RULE_ID_NO_RECENT_USER_CLICK;
import static com.mavenkalabs.adskipper.rules.RuleConstants.RULE_PARAM_LAST_USER_CLICK_TS;
import static com.mavenkalabs.adskipper.rules.RuleConstants.RULE_PARAM_QUIET_INTERVAL;

public class NoRecentUserClickRule implements BaseRule {
    @Override
    public RuleResult apply(AccessibilityNodeInfo node, Map<String, Object> parameters) {
        if (parameters != null) {
            Object value1 = parameters.get(RULE_PARAM_LAST_USER_CLICK_TS);
            Object value2 = parameters.get(RULE_PARAM_QUIET_INTERVAL);
            if (value1 instanceof Long && value2 instanceof Long) {
                long clickTS = (Long) value1;
                long quietInterval = (Long) value2;
                if (System.currentTimeMillis() > (clickTS + quietInterval)) {
                    return new RuleResult(true);
                }
            }
        }
        return new RuleResult(false);
    }

    @NonNull
    @Override
    public String toString() {
        return RULE_ID_NO_RECENT_USER_CLICK;
    }
}
