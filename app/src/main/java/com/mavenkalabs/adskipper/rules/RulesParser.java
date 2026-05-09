package com.mavenkalabs.adskipper.rules;

import java.util.Arrays;

public class RulesParser {
    public BaseRule parse(String ruleAsString, String packageName) {
        if (ruleAsString == null || ruleAsString.trim().isEmpty()) {
            throw new IllegalArgumentException();
        } else if (ruleAsString.contains("&")) {
            return new MultiRule(MultiRule.Conditionality.AND,
                    Arrays.stream(ruleAsString.split("&"))
                            .map((s) -> parse(s, packageName))
                            .toArray(BaseRule[]::new));
        } else {
            if (ruleAsString.startsWith(RuleConstants.RULE_ID_NO_RECENT_USER_CLICK)) {
                return new NoRecentUserClickRule(ruleAsString);
            } else if (ruleAsString.startsWith("!")) {
                return new MustNotExistRule(ruleAsString.substring(1), packageName);
            } else {
                return new MustExistRule(ruleAsString, packageName);
            }
        }
    }

    public BaseRule orRules(BaseRule... rules) {
        if (rules == null || rules.length == 0) {
            throw new IllegalArgumentException();
        } else if (rules.length == 1) {
            return rules[0];
        } else {
            return new MultiRule(MultiRule.Conditionality.OR, rules);
        }
    }
}
