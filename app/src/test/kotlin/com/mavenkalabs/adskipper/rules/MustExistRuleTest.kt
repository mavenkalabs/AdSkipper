package com.mavenkalabs.adskipper.rules

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

internal class MustExistRuleTest {
    @Test
    fun verifyCorrectParsingForNormalId() {
        val packageName = "testPackage"
        val id = "test"
        val rule = MustExistRule(id, packageName)
        assertEquals(rule.id, id)
        assertNull(rule.index)
    }

    @Test
    fun verifyCorrectParsingForIndexedId() {
        val packageName = "testPackage"
        val id = "test"
        val index = 1
        val rule = MustExistRule("$id[$index]", packageName)
        assertEquals(id, rule.id)
        assertEquals(index, rule.index)
    }
}
