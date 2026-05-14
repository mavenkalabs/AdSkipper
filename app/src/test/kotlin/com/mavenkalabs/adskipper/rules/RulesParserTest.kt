package com.mavenkalabs.adskipper.rules

import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import java.lang.String
import kotlin.IllegalArgumentException
import kotlin.test.assertEquals

internal class RulesParserTest {

    private lateinit var rulesParser: RulesParser

    @Before
    fun setupBefore() {
        rulesParser = RulesParser()
    }

    @Test
    fun verifyCorrectParsing() {
        val ruleAsString = "id1&!id2"
        val rule = rulesParser.parse(ruleAsString, "test.package")

        assertEquals(ruleAsString, rule.toString())
    }

    @Test
    fun verifyParsingWithBadArguments() {
        assertThrows(
            IllegalArgumentException::class.java
        ) { rulesParser.parse("  ", " ") }
    }

    @Test
    fun verifyOrRules() {
        val ruleAsString1 = "id1&!id2"
        val rule1 = rulesParser.parse(ruleAsString1, "test.package")
        val ruleAsString2 = String.join(
            ",",
            RULE_ID_NO_RECENT_USER_CLICK,
            1000.toString()
        )
        val rule2 = rulesParser.parse(ruleAsString2, "test.package")
        assertEquals(
            String.join("|", ruleAsString1, ruleAsString2),
            rulesParser.orRules(rule1, rule2).toString()
        )
    }

    @Test
    fun verifyOrRulesWithOneRule() {
        val ruleAsString1 = "id1&!id2"
        val rule1 = rulesParser.parse(ruleAsString1, "test.package")

        assertEquals(rule1, rulesParser.orRules(rule1))
    }

    @Test
    fun verifyOrRulesWithBadArguments() {
        assertThrows(
            IllegalArgumentException::class.java,
            { rulesParser.orRules() })
    }
}