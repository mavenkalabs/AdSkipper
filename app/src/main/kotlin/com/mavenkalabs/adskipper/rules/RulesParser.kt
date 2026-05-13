package com.mavenkalabs.adskipper.rules

class RulesParser {
    fun parse(ruleAsString: String, packageName: String): BaseRule {
        require(ruleAsString.trim().isNotEmpty())
        return if (ruleAsString.contains("&")) {
            MultiRule (
                MultiRule.Conditionality.AND,
                *ruleAsString.split("&".toRegex())
                    .map { s -> parse(s, packageName)  }
                    .toTypedArray())
        } else {
            if (ruleAsString.startsWith(RULE_ID_NO_RECENT_USER_CLICK)) {
                NoRecentUserClickRule(ruleAsString)
            } else if (ruleAsString.startsWith("!")) {
                MustNotExistRule(ruleAsString.substring(1), packageName)
            } else {
                MustExistRule(ruleAsString, packageName)
            }
        }
    }

    fun orRules(vararg rules: BaseRule): BaseRule {
        require(rules.isNotEmpty())
        return if (rules.size == 1) {
            rules.first()
        } else {
            MultiRule(MultiRule.Conditionality.OR, *rules)
        }
    }
}
