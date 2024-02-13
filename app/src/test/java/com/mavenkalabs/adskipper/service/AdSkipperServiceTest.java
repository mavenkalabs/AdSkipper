package com.mavenkalabs.adskipper.service;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import java.util.Collections;
import java.util.List;

import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class AdSkipperServiceTest {

    @Mock private AccessibilityEvent eventMock;

    @Mock private AccessibilityNodeInfo nodeInfoMock;

    @Mock private AudioManager audioManagerMock;

    @Mock private SharedPreferences sharedPreferencesMock;

    @Spy private AdSkipperService service;

    private AutoCloseable closeable;

    private static final String YT_PKG_NAME = "com.google.android.youtube";

    private static final String YT_MUSIC_PKG_NAME = "com.google.android.apps.youtube.music";

    @Before
    public void setupBefore() {
        closeable = MockitoAnnotations.openMocks(this);

        doReturn(audioManagerMock).when(service).getSystemService(eq(Context.AUDIO_SERVICE));
        doReturn(false).when(audioManagerMock).isStreamMute(eq(AudioManager.STREAM_MUSIC));

        Context contextMock = mock(Context.class);
        doReturn(contextMock).when(service).getApplicationContext();
        doReturn(AdSkipperService.class.getPackageName()).when(contextMock).getPackageName();
        doReturn(sharedPreferencesMock).when(contextMock).getSharedPreferences(anyString(), anyInt());
        when(sharedPreferencesMock.getBoolean(anyString(), anyBoolean())).thenReturn(true);

        service.onServiceConnected();
    }

    @After
    public void tearDownAfter() throws Exception {
        closeable.close();
    }

    @Test
    public void verifyEvtHandling() {

        when(eventMock.getSource()).thenReturn(nodeInfoMock);
        when(eventMock.getPackageName()).thenReturn(YT_PKG_NAME);
        when(nodeInfoMock.isClickable()).thenReturn(false);
        when(nodeInfoMock.findAccessibilityNodeInfosByViewId(eq(YT_PKG_NAME+":id/ad_progress_text")))
                .thenReturn(List.of(nodeInfoMock));

        service.onAccessibilityEvent(eventMock);

        verify(audioManagerMock, times(1))
                .adjustStreamVolume(eq(AudioManager.STREAM_MUSIC), eq(AudioManager.ADJUST_MUTE), eq(0));

        doReturn(true).when(audioManagerMock).isStreamMute(eq(AudioManager.STREAM_MUSIC));
        when(nodeInfoMock.findAccessibilityNodeInfosByViewId(eq(YT_PKG_NAME+":id/ad_progress_text")))
                .thenReturn(Collections.emptyList());
        when(nodeInfoMock.findAccessibilityNodeInfosByViewId(eq(YT_PKG_NAME+":id/skip_ad_button")))
                .thenReturn(List.of(nodeInfoMock));
        when(nodeInfoMock.isClickable()).thenReturn(true);

        service.onAccessibilityEvent(eventMock);

        verify(audioManagerMock, times(1))
                .adjustStreamVolume(eq(AudioManager.STREAM_MUSIC), eq(AudioManager.ADJUST_UNMUTE), eq(0));
        verify(nodeInfoMock, times(1))
                .performAction(eq(AccessibilityNodeInfo.ACTION_CLICK));
    }
    @Test()
    public void verifyEvtHandlingNullSource() {
        when(eventMock.getSource()).thenReturn(null);

        try {
            service.onAccessibilityEvent(eventMock);
        } catch (Throwable t) {
            fail("Unexpected exception thrown: " + t.getMessage());
        }
    }

    @Test()
    public void verifyEvtHandlingSourceNoChildren() {

        when(eventMock.getSource()).thenReturn(nodeInfoMock);
        when(eventMock.getPackageName()).thenReturn(YT_PKG_NAME);
        when(nodeInfoMock.findAccessibilityNodeInfosByViewId(anyString()))
                .thenReturn(Collections.emptyList());

        service.onAccessibilityEvent(eventMock);

        verify(nodeInfoMock, never())
                .performAction(eq(AccessibilityNodeInfo.ACTION_CLICK));
    }

    @Test()
    public void verifyEvtHandlingSourceChildrenNotClickable() {

        when(eventMock.getSource()).thenReturn(nodeInfoMock);
        when(eventMock.getPackageName()).thenReturn(YT_PKG_NAME);
        when(nodeInfoMock.isClickable()).thenReturn(false);

        service.onAccessibilityEvent(eventMock);

        verify(nodeInfoMock, never())
                .performAction(eq(AccessibilityNodeInfo.ACTION_CLICK));
    }

    @Test
    public void verifyEvtHandlingForYTMusic() {
        when(eventMock.getSource()).thenReturn(nodeInfoMock);
        when(eventMock.getPackageName()).thenReturn(YT_MUSIC_PKG_NAME);
        when(nodeInfoMock.isClickable()).thenReturn(false);
        when(nodeInfoMock.findAccessibilityNodeInfosByViewId(eq(YT_MUSIC_PKG_NAME+":id/player_learn_more_button")))
                .thenReturn(List.of(nodeInfoMock));

        service.onAccessibilityEvent(eventMock);

        verify(audioManagerMock, times(1))
                .adjustStreamVolume(eq(AudioManager.STREAM_MUSIC), eq(AudioManager.ADJUST_MUTE), eq(0));

        doReturn(true).when(audioManagerMock).isStreamMute(eq(AudioManager.STREAM_MUSIC));
        when(nodeInfoMock.findAccessibilityNodeInfosByViewId(eq(YT_MUSIC_PKG_NAME+":id/player_learn_more_button")))
                .thenReturn(Collections.emptyList());
        when(nodeInfoMock.findAccessibilityNodeInfosByViewId(eq(YT_MUSIC_PKG_NAME+":id/skip_ad_button")))
                .thenReturn(List.of(nodeInfoMock));
        when(nodeInfoMock.isClickable()).thenReturn(true);

        service.onAccessibilityEvent(eventMock);

        verify(audioManagerMock, times(1))
                .adjustStreamVolume(eq(AudioManager.STREAM_MUSIC), eq(AudioManager.ADJUST_UNMUTE), eq(0));
        verify(nodeInfoMock, times(1))
                .performAction(eq(AccessibilityNodeInfo.ACTION_CLICK));
    }

    @Test
    public void verifyEvtHandlingForUnsupportedPkg() {
        when(eventMock.getPackageName()).thenReturn("com.google.android.apps.notube");
        service.onAccessibilityEvent(eventMock);

        verify(nodeInfoMock, never())
                .performAction(eq(AccessibilityNodeInfo.ACTION_CLICK));
    }
    @Test
    public void verifyOverriddenMethods() {
        service.onInterrupt();

        service.onUnbind(new Intent());
    }

    @Test
    public void verifyEvtHandlingWithMultipleEvents() throws InterruptedException {
        when(eventMock.getSource()).thenReturn(nodeInfoMock);
        when(eventMock.getPackageName()).thenReturn(YT_PKG_NAME);
        when(nodeInfoMock.findAccessibilityNodeInfosByViewId(eq(YT_PKG_NAME+":id/ad_progress_text")))
                .thenReturn(List.of(nodeInfoMock));

        service.onAccessibilityEvent(eventMock);

        reset(nodeInfoMock);
        doReturn(true).when(audioManagerMock).isStreamMute(eq(AudioManager.STREAM_MUSIC));
        when(nodeInfoMock.findAccessibilityNodeInfosByViewId(eq(YT_PKG_NAME+":id/skip_ad_button")))
                .thenReturn(List.of(nodeInfoMock));
        when(nodeInfoMock.isClickable()).thenReturn(true);

        service.onAccessibilityEvent(eventMock);

        reset(nodeInfoMock);
        doReturn(false).when(audioManagerMock).isStreamMute(eq(AudioManager.STREAM_MUSIC));
        when(nodeInfoMock.findAccessibilityNodeInfosByViewId(eq(YT_PKG_NAME+":id/ad_progress_text")))
                .thenReturn(List.of(nodeInfoMock));

        service.onAccessibilityEvent(eventMock);

        reset(nodeInfoMock);
        doReturn(false).when(audioManagerMock).isStreamMute(eq(AudioManager.STREAM_MUSIC));
        when(nodeInfoMock.findAccessibilityNodeInfosByViewId(eq(YT_PKG_NAME+":id/skip_ad_button")))
                .thenReturn(List.of(nodeInfoMock));
        when(nodeInfoMock.isClickable()).thenReturn(true);

        service.onAccessibilityEvent(eventMock);

        Thread.sleep(2000);

        verify(audioManagerMock, times(1))
                .adjustStreamVolume(eq(AudioManager.STREAM_MUSIC), eq(AudioManager.ADJUST_MUTE), eq(0));
        verify(audioManagerMock, times(1))
                .adjustStreamVolume(eq(AudioManager.STREAM_MUSIC), eq(AudioManager.ADJUST_UNMUTE), eq(0));
        verify(nodeInfoMock, times(1))
                .performAction(eq(AccessibilityNodeInfo.ACTION_CLICK));
    }
}