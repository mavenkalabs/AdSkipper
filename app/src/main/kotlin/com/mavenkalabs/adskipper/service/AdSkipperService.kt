package com.mavenkalabs.adskipper.service

import android.accessibilityservice.AccessibilityService
import android.annotation.SuppressLint
import android.content.Intent
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.media.AudioManager
import android.view.accessibility.AccessibilityEvent
import androidx.preference.PreferenceManager
import com.mavenkalabs.adskipper.service.Handler.Companion.extrasRef
import com.mavenkalabs.adskipper.util.AppLog
import com.mavenkalabs.adskipper.util.AppLog.Companion.e
import com.mavenkalabs.adskipper.util.EXTRA_PREF_MUTE_ADS
import java.util.concurrent.atomic.AtomicReference

@SuppressLint("AccessibilityPolicy")
class AdSkipperService : AccessibilityService() {
    private var preferenceChangeListener : OnSharedPreferenceChangeListener? = null

    private val configRef = AtomicReference<MutableMap<String, List<Handler>>>()

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        val eventHandlers = configRef.get()[event.packageName]
        eventHandlers?.forEach {
            it.apply(this, event)
        }
    }

    private fun unMute() {
        val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        val isCurrentlyMuted = audioManager.isStreamMute(AudioManager.STREAM_MUSIC)
        if (isCurrentlyMuted) {
            audioManager.adjustStreamVolume(
                AudioManager.STREAM_MUSIC,
                AudioManager.ADJUST_UNMUTE, 0
            )
        }
    }

    override fun onUnbind(intent: Intent?): Boolean {
        unMute()
        return true
    }

    override fun onInterrupt() {
        unMute()
    }

    override fun onDestroy() {
        super.onDestroy()

        try {
            if (preferenceChangeListener != null) {
                val prefs = PreferenceManager.getDefaultSharedPreferences(applicationContext)
                prefs.unregisterOnSharedPreferenceChangeListener(preferenceChangeListener)
            }
        } catch (e: Exception) {
            e(TAG, e.message?:"Unknown error", e)

        }
    }

    private fun processPrefs(prefs: SharedPreferences) {
        val extras =  extrasRef.get()
        extras[EXTRA_PREF_MUTE_ADS] = prefs.getBoolean(MUTE_ADS_PREF, true)
        AppLog.d(TAG, "onServiceConnected: pref  {0} changed to {1}",
            MUTE_ADS_PREF, extras[EXTRA_PREF_MUTE_ADS])
        extrasRef.set(extras)
    }

    public override fun onServiceConnected() {
        val prefs = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        processPrefs(prefs)
        prefs.registerOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener { p: SharedPreferences?, _: String? ->
            p?.let {
                processPrefs(p)
            }
        }.also { preferenceChangeListener = it })

        configRef.set( loadConfig(applicationContext).toMutableMap())
        AppLog.enable(null)
        AppLog.d(TAG, "Config is set to {0}", configRef.get())
    }

    companion object {
        const val MUTE_ADS_PREF: String = "mute_ads"

        const val CAPTURE_LOGS_PREF: String = "enable_logging"

        private val TAG: String = AdSkipperService::class.java.name
    }
}
