package com.mavenkalabs.adskipper.rules

import android.view.accessibility.AccessibilityNodeInfo
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.whenever
import java.lang.AutoCloseable
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NoRecentUserClickRuleTest {

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
    fun verifyCorrectApply() {
        val rule = NoRecentUserClickRule("test,1000")
        var result = rule.apply(nodeInfoMock, mapOf(
            RULE_PARAM_LAST_USER_CLICK_TS to System.currentTimeMillis()
        ))
        assertFalse { result.isPassed }

        result = rule.apply(nodeInfoMock, mapOf(
            RULE_PARAM_LAST_USER_CLICK_TS to (System.currentTimeMillis()-2000)
        ))
        assertTrue { result.isPassed }
    }
}