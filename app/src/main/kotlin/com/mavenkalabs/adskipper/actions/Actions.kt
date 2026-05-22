package com.mavenkalabs.adskipper.actions

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.accessibilityservice.GestureDescription.StrokeDescription
import android.content.Context.AUDIO_SERVICE
import android.graphics.Path
import android.graphics.Rect
import android.media.AudioManager
import android.view.ViewConfiguration
import android.view.accessibility.AccessibilityNodeInfo
import com.mavenkalabs.adskipper.util.AppLog
import com.mavenkalabs.adskipper.util.EXTRA_LAST_APP_CLICK_TS
import com.mavenkalabs.adskipper.util.EXTRA_LAST_USER_CLICK_TS
import com.mavenkalabs.adskipper.util.EXTRA_PREF_MUTE_ADS
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.concurrent.atomic.AtomicReference

@Serializable
sealed interface Action {
    fun apply(service: AccessibilityService,
              extras: MutableMap<String, Any>? = null,
              node: AccessibilityNodeInfo? = null): Boolean
}

@Serializable
@SerialName("LogUserTap")
class LogUserTap: Action {
    override fun apply(
        service: AccessibilityService,
        extras: MutableMap<String, Any>?,
        node: AccessibilityNodeInfo?
    ): Boolean {
        AppLog.d(TAG, "Applying LogUserTap action")
        val now = System.currentTimeMillis()
        val lastAppClickTS = (extras?.get(EXTRA_LAST_APP_CLICK_TS) as Long?)?:0
        // ignore click events generated within a quiet interval
        if ((now - lastAppClickTS) > APP_CLICK_QUIET_INTERVAL) {
            extras?.put(EXTRA_LAST_USER_CLICK_TS, now)
            AppLog.d(
                TAG,
                "Detected user tap on {0}",
                node?.viewIdResourceName
            )
        }

        return true
    }

    override fun toString(): String {
        return "LogUserTap()"
    }

    companion object {
        private const val APP_CLICK_QUIET_INTERVAL: Int = 1000
        private val TAG = LogUserTap::class.java.name

    }
}
@Serializable
@SerialName("Mute")
class Mute(val mute: Boolean) : Action {
    private val logTag = Mute::class.java.name

    override fun apply(
        service: AccessibilityService,
        extras: MutableMap<String, Any>?,
        node: AccessibilityNodeInfo?
    ): Boolean {
        if (extras?.get(EXTRA_PREF_MUTE_ADS) == true) {
            if (isCurrentlyMutedRef.get() == null) {
                val audioManager = service.getSystemService(AUDIO_SERVICE) as AudioManager
                isCurrentlyMutedRef.set(audioManager.isStreamMute(AudioManager.STREAM_MUSIC))
            }

            if (isCurrentlyMutedRef.get() != mute) {
                val audioManager = service.getSystemService(AUDIO_SERVICE) as AudioManager
                audioManager.adjustStreamVolume(
                    AudioManager.STREAM_MUSIC,
                    if (mute) AudioManager.ADJUST_MUTE else AudioManager.ADJUST_UNMUTE, 0
                )
                isCurrentlyMutedRef.set(mute)
                AppLog.d(logTag, "Toggling mute to {0}", mute)
            }
            return true
        } else {
            return false
        }
    }

    override fun toString(): String {
        return "Mute(mute=$mute)"
    }

    companion object {
        private val isCurrentlyMutedRef = AtomicReference<Boolean>()
    }
}

@Serializable
@SerialName("Tap")
class Tap : Action {
    private val logTag = Tap::class.java.name

    override fun apply(
        service: AccessibilityService,
        extras: MutableMap<String, Any>?,
        node: AccessibilityNodeInfo?
    ): Boolean {
        val now = System.currentTimeMillis()
        val lastAppClickTS = extras?.get(EXTRA_LAST_APP_CLICK_TS) as Long??:0
        if ((now - lastAppClickTS) <= APP_CLICK_QUIET_INTERVAL) {
            return false
        }

        extras?.put(EXTRA_LAST_APP_CLICK_TS, now)

        return node?.let {
            if (node.isClickable && node.isEnabled) {
                val nodeBounds = Rect()
                node.getBoundsInScreen(nodeBounds)
                val tapPath = Path()
                tapPath.moveTo(nodeBounds.centerX().toFloat(), nodeBounds.centerY().toFloat())
                val tapStroke =
                    StrokeDescription(tapPath, 0, ViewConfiguration.getTapTimeout().toLong())

                val gestureBuilder = GestureDescription.Builder()
                gestureBuilder.addStroke(tapStroke)
                val success = service.dispatchGesture(gestureBuilder.build(), null, null)

                AppLog.d(logTag, "Tapped on {0}", node.viewIdResourceName)

                // try click action if gesture doesn't succeed
                if (!success) {
                    AppLog.d(logTag, "Click gesture didn't work. Using performAction")
                    return node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                } else {
                    return true
                }
            } else {
                return false
            }
        }?:false
    }

    override fun toString(): String {
        return "Tap()"
    }

    companion object {
        private const val APP_CLICK_QUIET_INTERVAL = 1000
    }
}