package com.mavenkalabs.adskipper.service;

import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Collections;

import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class AdSkipperServiceTest {

    private AdSkipperService service;

    @Mock private AccessibilityEvent eventMock;

    private AutoCloseable closeable;

    @Before
    public void setupBefore() {
        closeable = MockitoAnnotations.openMocks(this);
        service = new AdSkipperService();

    }

    @After
    public void tearDownAfter() throws Exception {
        closeable.close();
    }

    @Test
    public void verifyEvtHandling() {
        AccessibilityNodeInfo nodeInfoMock = mock(AccessibilityNodeInfo.class);
        when(eventMock.getSource()).thenReturn(nodeInfoMock);
        when(nodeInfoMock.findAccessibilityNodeInfosByViewId(anyString()))
                .thenReturn(Collections.singletonList(nodeInfoMock));
        when(nodeInfoMock.isClickable()).thenReturn(true);
        service.onAccessibilityEvent(eventMock);

        verify(nodeInfoMock, times(1))
                .performAction(eq(AccessibilityNodeInfo.ACTION_CLICK));
    }
    @Test()
    public void verifyEvtHandlingNullSource() {
        try {
            service.onAccessibilityEvent(eventMock);
        } catch (Throwable t) {
            fail("Unexpected exception thrown" + t.getMessage());
        }
    }

    @Test()
    public void verifyEvtHandlingSourceNoChildren() {
        AccessibilityNodeInfo nodeInfoMock = mock(AccessibilityNodeInfo.class);
        when(eventMock.getSource()).thenReturn(nodeInfoMock);
        when(nodeInfoMock.findAccessibilityNodeInfosByViewId(anyString()))
                .thenReturn(Collections.emptyList());
        service.onAccessibilityEvent(eventMock);
        verify(nodeInfoMock, never())
                .performAction(eq(AccessibilityNodeInfo.ACTION_CLICK));
    }

    @Test()
    public void verifyEvtHandlingSourceChildrenNotClickable() {
        AccessibilityNodeInfo nodeInfoMock = mock(AccessibilityNodeInfo.class);
        when(eventMock.getSource()).thenReturn(nodeInfoMock);
        when(nodeInfoMock.findAccessibilityNodeInfosByViewId(anyString()))
                .thenReturn(Collections.singletonList(nodeInfoMock));
        when(nodeInfoMock.isClickable()).thenReturn(false);
        service.onAccessibilityEvent(eventMock);
        verify(nodeInfoMock, never())
                .performAction(eq(AccessibilityNodeInfo.ACTION_CLICK));
    }
}