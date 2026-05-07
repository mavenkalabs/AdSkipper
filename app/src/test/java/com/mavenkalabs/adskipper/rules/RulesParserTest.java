package com.mavenkalabs.adskipper.rules;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class RulesParserTest {

    private RulesParser rulesParser;

    @Before
    public void setupBefore() {
        rulesParser = new RulesParser();
    }

    @Test
    public void verifyCorrectParsing() {
        String ruleAsString = "id1&!id2";
        BaseRule rule = rulesParser.parse(ruleAsString, "test.package");

        Assert.assertEquals(ruleAsString, rule.toString());
    }

    @Test
    public void verifyParsingWithBadArguments() {
        Assert.assertThrows(IllegalArgumentException.class,
                () -> rulesParser.parse(null, null));
        Assert.assertThrows(IllegalArgumentException.class,
                () -> rulesParser.parse("  ", null));
        Assert.assertThrows(IllegalArgumentException.class,
                () -> rulesParser.parse(null, " "));
    }

    @Test
    public void verifyOrRules() {
        String ruleAsString1 = "id1&!id2";
        BaseRule rule1 = rulesParser.parse(ruleAsString1, "test.package");
        String ruleAsString2 = String.join(",",
                RuleConstants.RULE_ID_NO_RECENT_USER_CLICK,
                String.valueOf(1000));
        BaseRule rule2 = rulesParser.parse(ruleAsString2, "test.package");
        Assert.assertEquals(String.join("|", ruleAsString1, ruleAsString2),
                rulesParser.orRules(rule1, rule2).toString());
    }

    @Test
    public void verifyOrRulesWithOneRule() {
        String ruleAsString1 = "id1&!id2";
        BaseRule rule1 = rulesParser.parse(ruleAsString1, "test.package");

        Assert.assertEquals(rule1, rulesParser.orRules(rule1));
    }

    @Test
    public void verifyOrRulesWithBadArguments() {
        Assert.assertThrows(IllegalArgumentException.class,
                () -> rulesParser.orRules());
    }
}
