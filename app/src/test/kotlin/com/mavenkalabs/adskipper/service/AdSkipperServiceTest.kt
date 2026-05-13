package com.mavenkalabs.adskipper.service

import android.accessibilityservice.AccessibilityService.GestureResultCallback
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.media.AudioManager
import android.os.Handler
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.eq
import org.mockito.Captor
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.doReturn
import org.mockito.Mockito.reset
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.MockitoAnnotations
import org.mockito.Spy
import org.mockito.kotlin.whenever
import java.lang.AutoCloseable

private
const val YT_PKG_NAME: String = "com.google.android.youtube"

private
const val YT_MUSIC_PKG_NAME: String = "com.google.android.apps.youtube.music"


internal class AdSkipperServiceTest {

    @Mock
    private lateinit var eventMock: AccessibilityEvent

    @Mock
    private lateinit var nodeInfoMock: AccessibilityNodeInfo

    @Mock
    private lateinit var audioManagerMock: AudioManager

    @Mock
    private lateinit var sharedPreferencesMock: SharedPreferences

    @Spy
    private lateinit var service: AdSkipperService

    @Captor
    private var listenerArgumentCaptor: ArgumentCaptor<OnSharedPreferenceChangeListener?>? = null

    private var closeable: AutoCloseable? = null

    @Before
    fun setupBefore() {
        closeable = MockitoAnnotations.openMocks(this)

        doReturn(audioManagerMock).whenever(service)
            .getSystemService(eq(Context.AUDIO_SERVICE))
        doReturn(false).whenever(audioManagerMock)
            .isStreamMute(ArgumentMatchers.eq(AudioManager.STREAM_MUSIC))

        val contextMock = Mockito.mock(Context::class.java)
        doReturn(contextMock).whenever(service).applicationContext
        doReturn(AdSkipperService::class.java.packageName)
            .whenever(contextMock).packageName
        doReturn(sharedPreferencesMock).whenever(contextMock)
            .getSharedPreferences(ArgumentMatchers.anyString(), ArgumentMatchers.anyInt())
        whenever(
            sharedPreferencesMock.getBoolean(
                ArgumentMatchers.anyString(),
                ArgumentMatchers.anyBoolean()
            )
        ).thenReturn(true)

        doReturn(nodeInfoMock).whenever(service).rootInActiveWindow
        whenever(eventMock.packageName).thenReturn(YT_PKG_NAME)
        whenever(nodeInfoMock.isClickable).thenReturn(false)
        whenever(nodeInfoMock.isVisibleToUser).thenReturn(false)
        whenever(nodeInfoMock.isEnabled).thenReturn(false)
        doReturn(false).whenever(audioManagerMock)
            .isStreamMute(ArgumentMatchers.eq(AudioManager.STREAM_MUSIC))
        whenever(
            nodeInfoMock.findAccessibilityNodeInfosByViewId(
                ArgumentMatchers.anyString()
            )
        ).thenReturn(mutableListOf())

        service.onServiceConnected()
    }

    @After
    fun tearDownAfter() {
        closeable?.close()
    }

    @Test
    fun verifyEvtHandling() {
        whenever(
            nodeInfoMock.findAccessibilityNodeInfosByViewId(
                eq("$YT_PKG_NAME:id/ad_progress_text")
            )
        )
            .thenReturn(listOf(nodeInfoMock))

        service.onAccessibilityEvent(eventMock)

        verify(audioManagerMock, times(1))
            .adjustStreamVolume(
                ArgumentMatchers.eq(AudioManager.STREAM_MUSIC),
                ArgumentMatchers.eq(AudioManager.ADJUST_MUTE),
                ArgumentMatchers.eq(0)
            )

        doReturn(true).whenever(audioManagerMock)
            .isStreamMute(ArgumentMatchers.eq(AudioManager.STREAM_MUSIC))
        whenever(
            nodeInfoMock.findAccessibilityNodeInfosByViewId(
                eq("$YT_PKG_NAME:id/ad_progress_text")
            )
        )
            .thenReturn(listOf())
        whenever(
            nodeInfoMock.findAccessibilityNodeInfosByViewId(
                eq("$YT_PKG_NAME:id/skip_ad_button")
            )
        )
            .thenReturn(listOf(nodeInfoMock))
        whenever(nodeInfoMock.isClickable).thenReturn(true)
        whenever(nodeInfoMock.isVisibleToUser).thenReturn(true)
        whenever(nodeInfoMock.isEnabled).thenReturn(true)

        service.onAccessibilityEvent(eventMock)

        verify(audioManagerMock, times(1))
            .adjustStreamVolume(
                ArgumentMatchers.eq(AudioManager.STREAM_MUSIC),
                ArgumentMatchers.eq(AudioManager.ADJUST_UNMUTE),
                ArgumentMatchers.eq(0)
            )
        verify(service, times(1)).dispatchGesture(
            ArgumentMatchers.any(),
            ArgumentMatchers.any<GestureResultCallback?>(),
            ArgumentMatchers.any<Handler?>()
        )
    }


    @Test
    fun verifyEvtHandlingWithDisabledButton() {
        whenever(
            nodeInfoMock.findAccessibilityNodeInfosByViewId(
                eq("$YT_PKG_NAME:id/skip_ad_button")
            )
        )
            .thenReturn(listOf<AccessibilityNodeInfo?>(nodeInfoMock))
        whenever(nodeInfoMock.isClickable).thenReturn(true)
        whenever(nodeInfoMock.isVisibleToUser).thenReturn(true)
        whenever(nodeInfoMock.isEnabled).thenReturn(false)

        service.onAccessibilityEvent(eventMock)
        verify(nodeInfoMock, Mockito.never())
            .performAction(ArgumentMatchers.anyInt())
        verify(service, Mockito.never()).dispatchGesture(
            ArgumentMatchers.any(),
            ArgumentMatchers.any<GestureResultCallback?>(),
            ArgumentMatchers.any<Handler?>()
        )
    }

    @Test
    fun verifyEvtHandlingNullSource() {
        whenever(eventMock.source).thenReturn(null)

        try {
            service.onAccessibilityEvent(eventMock)
        } catch (t: Throwable) {
            Assert.fail("Unexpected exception thrown: " + t.message)
        }
    }

    @Test
    fun verifyEvtHandlingSourceNoChildren() {
        service.onAccessibilityEvent(eventMock)

        verify(nodeInfoMock, Mockito.never())
            .performAction(ArgumentMatchers.eq(AccessibilityNodeInfo.ACTION_CLICK))
    }

    @Test
    fun verifyEvtHandlingSourceChildrenNotClickable() {
        whenever(
            nodeInfoMock.findAccessibilityNodeInfosByViewId(
                eq("$YT_PKG_NAME:id/skip_ad_button")
            )
        )
            .thenReturn(listOf(nodeInfoMock))

        service.onAccessibilityEvent(eventMock)

        verify(nodeInfoMock, Mockito.never())
            .performAction(ArgumentMatchers.eq(AccessibilityNodeInfo.ACTION_CLICK))
    }

    @Test
    fun verifyEvtHandlingForYTMusic() {
        whenever(eventMock.packageName).thenReturn(YT_MUSIC_PKG_NAME)
        whenever(
            nodeInfoMock.findAccessibilityNodeInfosByViewId(
                eq("$YT_MUSIC_PKG_NAME:id/player_learn_more_button")
            )
        )
            .thenReturn(listOf(nodeInfoMock))

        service.onAccessibilityEvent(eventMock)

        verify(audioManagerMock, times(1))
            .adjustStreamVolume(
                ArgumentMatchers.eq(AudioManager.STREAM_MUSIC),
                ArgumentMatchers.eq(AudioManager.ADJUST_MUTE),
                ArgumentMatchers.eq(0)
            )

        doReturn(true).whenever(audioManagerMock)
            .isStreamMute(ArgumentMatchers.eq(AudioManager.STREAM_MUSIC))
        whenever(
            nodeInfoMock.findAccessibilityNodeInfosByViewId(
                eq("$YT_MUSIC_PKG_NAME:id/player_learn_more_button")
            )
        )
            .thenReturn(mutableListOf<AccessibilityNodeInfo?>())
        whenever(
            nodeInfoMock.findAccessibilityNodeInfosByViewId(
                eq("$YT_MUSIC_PKG_NAME:id/skip_ad_button")
            )
        )
            .thenReturn(listOf(nodeInfoMock))
        whenever(nodeInfoMock.isClickable).thenReturn(true)
        whenever(nodeInfoMock.isEnabled).thenReturn(true)
        whenever(nodeInfoMock.isVisibleToUser).thenReturn(true)

        service.onAccessibilityEvent(eventMock)

        verify(audioManagerMock, times(1))
            .adjustStreamVolume(
                ArgumentMatchers.eq(AudioManager.STREAM_MUSIC),
                ArgumentMatchers.eq(AudioManager.ADJUST_UNMUTE),
                ArgumentMatchers.eq(0)
            )
        verify(service, times(1)).dispatchGesture(
            ArgumentMatchers.any(),
            ArgumentMatchers.any<GestureResultCallback?>(),
            ArgumentMatchers.any<Handler?>()
        )
    }

    @Test
    fun verifyEvtHandlingForUnsupportedPkg() {
        whenever(eventMock.packageName)
            .thenReturn("com.google.android.apps.notube")
        service.onAccessibilityEvent(eventMock)

        verify(nodeInfoMock, Mockito.never())
            .performAction(ArgumentMatchers.eq(AccessibilityNodeInfo.ACTION_CLICK))
    }

    @Test
    fun verifyOverriddenMethods() {
        service.onInterrupt()

        service.onUnbind(Intent())
    }

    @Test
    fun verifyEvtHandlingWithMultipleEvents() {
        whenever(
            nodeInfoMock.findAccessibilityNodeInfosByViewId(
                eq("$YT_PKG_NAME:id/ad_progress_text")
            )
        )
            .thenReturn(listOf(nodeInfoMock))

        service.onAccessibilityEvent(eventMock)

        reset<AccessibilityNodeInfo?>(nodeInfoMock)
        doReturn(true).whenever(audioManagerMock)
            .isStreamMute(ArgumentMatchers.eq(AudioManager.STREAM_MUSIC))
        whenever(
            nodeInfoMock.findAccessibilityNodeInfosByViewId(
                eq("$YT_PKG_NAME:id/skip_ad_button")
            )
        )
            .thenReturn(listOf(nodeInfoMock))
        whenever(nodeInfoMock.isClickable).thenReturn(true)
        whenever(nodeInfoMock.isVisibleToUser).thenReturn(true)
        whenever(nodeInfoMock.isEnabled).thenReturn(true)

        service.onAccessibilityEvent(eventMock)

        verify(service, times(1)).dispatchGesture(
            ArgumentMatchers.any(),
            ArgumentMatchers.any<GestureResultCallback?>(),
            ArgumentMatchers.any<Handler?>()
        )

        reset<AccessibilityNodeInfo?>(nodeInfoMock)
        doReturn(false).whenever(audioManagerMock)
            .isStreamMute(ArgumentMatchers.eq(AudioManager.STREAM_MUSIC))
        whenever(
            nodeInfoMock.findAccessibilityNodeInfosByViewId(
                eq("$YT_PKG_NAME:id/ad_progress_text")
            )
        )
            .thenReturn(listOf(nodeInfoMock))

        service.onAccessibilityEvent(eventMock)

        reset(nodeInfoMock)
        doReturn(false).whenever(audioManagerMock)
            .isStreamMute(ArgumentMatchers.eq(AudioManager.STREAM_MUSIC))
        whenever(
            nodeInfoMock.findAccessibilityNodeInfosByViewId(
                eq("$YT_PKG_NAME:id/skip_ad_button")
            )
        )
            .thenReturn(listOf(nodeInfoMock))
        whenever(nodeInfoMock.isClickable).thenReturn(true)
        whenever(nodeInfoMock.isVisibleToUser).thenReturn(true)
        whenever(nodeInfoMock.isEnabled).thenReturn(true)

        service.onAccessibilityEvent(eventMock)

        verify(audioManagerMock, times(2))
            .adjustStreamVolume(
                ArgumentMatchers.eq(AudioManager.STREAM_MUSIC),
                ArgumentMatchers.eq(AudioManager.ADJUST_MUTE),
                ArgumentMatchers.eq(0)
            )
        verify(audioManagerMock, times(1))
            .adjustStreamVolume(
                ArgumentMatchers.eq(AudioManager.STREAM_MUSIC),
                ArgumentMatchers.eq(AudioManager.ADJUST_UNMUTE),
                ArgumentMatchers.eq(0)
            )
        verify(nodeInfoMock, times(1))
            .performAction(ArgumentMatchers.eq(AccessibilityNodeInfo.ACTION_CLICK))
    }

    @Test
    fun verifyMutePreferenceChangeDynamicUpdate() {
        verify(sharedPreferencesMock)
            .registerOnSharedPreferenceChangeListener(
                listenerArgumentCaptor!!.capture()
            )

        doReturn(false).whenever(sharedPreferencesMock)
            .getBoolean(AdSkipperService.MUTE_ADS_PREF, false)
        listenerArgumentCaptor!!.getValue()!!
            .onSharedPreferenceChanged(sharedPreferencesMock, AdSkipperService.MUTE_ADS_PREF)

        whenever(
            nodeInfoMock.findAccessibilityNodeInfosByViewId(
                eq("$YT_PKG_NAME:id/ad_progress_text")
            )
        )
            .thenReturn(listOf(nodeInfoMock))

        service.onAccessibilityEvent(eventMock)

        verify(audioManagerMock, Mockito.never())
            .adjustStreamVolume(
                ArgumentMatchers.eq(AudioManager.STREAM_MUSIC),
                ArgumentMatchers.anyInt(),
                ArgumentMatchers.anyInt()
            )
    }
}