package com.mavenkalabs.adskipper.rules

import android.view.accessibility.AccessibilityNodeInfo
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.endsWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.lang.AutoCloseable
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MustNotExistRuleTest {

    @Mock
    private lateinit var nodeInfoMock: AccessibilityNodeInfo


    private var closeable: AutoCloseable? = null

    @Before
    fun setupBefore() {
        closeable = MockitoAnnotations.openMocks(this)

        whenever(nodeInfoMock.isClickable).thenReturn(false)
        whenever(nodeInfoMock.isVisibleToUser).thenReturn(false)
        whenever(nodeInfoMock.isEnabled).thenReturn(false)
    }

    @After
    fun tearDownAfter() {
        closeable?.close()
    }

    @Test
    fun verifyCorrectParsingForNormalId() {
        val packageName = "testPackage"
        val id = "test"
        val rule = MustNotExistRule(id, packageName)
        assertEquals(rule.id, id)
        assertNull(rule.index)
    }

    @Test
    fun verifyCorrectParsingForIndexedId() {
        val packageName = "testPackage"
        val id = "test"
        val index = 1
        val rule = MustNotExistRule("$id[$index]", packageName)
        assertEquals(id, rule.id)
        assertEquals(index, rule.index)
    }

    @Test
    fun verifyCorrectApply() {
        val mockResultNode = mock<AccessibilityNodeInfo>()
        whenever { nodeInfoMock.findAccessibilityNodeInfosByViewId(endsWith("test_id")) }
            .thenReturn(listOf(mockResultNode))

        val packageName = "testPackage"
        var rule = MustNotExistRule("test_id", packageName)
        var result = rule.apply(nodeInfoMock)
        assertFalse { result.isPassed }

        rule = MustNotExistRule("test_id[0]", packageName)
        result = rule.apply(nodeInfoMock)
        assertFalse { result.isPassed }

        rule = MustNotExistRule("test_id[1]", packageName)
        result = rule.apply(nodeInfoMock)
        assertTrue { result.isPassed }

        rule = MustNotExistRule("unk_id[0]", packageName)
        result = rule.apply(nodeInfoMock)
        assertTrue { result.isPassed }

        rule = MustNotExistRule("unk_id", packageName)
        result = rule.apply(nodeInfoMock)
        assertTrue { result.isPassed }
    }
}