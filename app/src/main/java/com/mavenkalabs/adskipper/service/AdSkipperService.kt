package com.mavenkalabs.adskipper.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.accessibilityservice.GestureDescription.StrokeDescription
import android.content.Intent
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.graphics.Path
import android.graphics.Rect
import android.media.AudioManager
import android.view.ViewConfiguration
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.preference.PreferenceManager
import com.mavenkalabs.adskipper.rules.BaseRule
import com.mavenkalabs.adskipper.rules.RuleConstants
import com.mavenkalabs.adskipper.rules.RulesParser
import com.mavenkalabs.adskipper.util.AppLog
import com.mavenkalabs.adskipper.util.AppLog.Companion.disable
import com.mavenkalabs.adskipper.util.AppLog.Companion.e
import com.mavenkalabs.adskipper.util.AppLog.Companion.enable
import com.mavenkalabs.adskipper.util.AppLog.Companion.logAccessibilityEvent
import com.mavenkalabs.adskipper.util.ConfigReader
import java.util.concurrent.atomic.AtomicReference
import java.util.function.Consumer

class AdSkipperService : AccessibilityService() {
    private var muteAds = false

    private var captureLogs = false

    private var adInProgress = false

    private val configReader: ConfigReader? = null

    private var lastClickTS = 0L

    private val listenerRef = AtomicReference<OnSharedPreferenceChangeListener?>()

    private val packageClickRules = AtomicReference<MutableMap<String, BaseRule>?>()

    private val packageMuteRules = AtomicReference<MutableMap<String, BaseRule>?>()

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        try {
            if (event.eventType == AccessibilityEvent.TYPE_VIEW_CLICKED) {
                lastClickTS = System.currentTimeMillis()
                return
            }

            val parameters = mutableMapOf(
                RuleConstants.RULE_PARAM_LAST_USER_CLICK_TS to lastClickTS
            )

            val eventPkgName =
                (if (event.packageName != null) event.packageName.toString() else null)
            val rootNode = rootInActiveWindow
            val currentMuteRules = packageMuteRules.get()!![eventPkgName]
            if (rootNode != null && currentMuteRules != null) {
                val conditionSatisfied = currentMuteRules.apply(rootNode, parameters).isPassed
                if (conditionSatisfied) {
                    if (muteAds && !adInProgress) {
                        toggleMute(true)
                        adInProgress = true
                        AppLog.d(TAG, "onAccessibilityEvent: Detected ad")
                        if (captureLogs) {
                            logAccessibilityEvent(rootInActiveWindow, AppLog.EventType.AD)
                        }
                    }
                }

                if (muteAds && !conditionSatisfied) {
                    if (adInProgress) {
                        toggleMute(false)
                        adInProgress = false
                    }
                }
            }

            val currentClickRules = packageClickRules.get()!![eventPkgName]
            if (rootNode != null && currentClickRules != null) {
                val result = currentClickRules.apply(rootNode, parameters)

                val conditionSatisfied = result.isPassed
                val foundNodes = result.filteredNodes
                if (conditionSatisfied && foundNodes != null && !foundNodes.isEmpty()) {
                    foundNodes.stream()
                        .filter { it.isClickable && it.isEnabled }
                        .findFirst()
                        .ifPresent(Consumer { accessibilityNodeInfo: AccessibilityNodeInfo ->
                            tap(accessibilityNodeInfo)
                            AppLog.d(
                                TAG,
                                "onAccessibilityEvent: Skipped ad {0}",
                                accessibilityNodeInfo.viewIdResourceName
                            )
                            if (captureLogs) {
                                logAccessibilityEvent(
                                    rootInActiveWindow,
                                    AppLog.EventType.SKIP
                                )
                            }
                        })
                }
            }
        } catch (e: Exception) {
            e(TAG, "Unexpected error", e)
        }
    }

    private fun tap(node: AccessibilityNodeInfo) {
        //try click action first
        val success = node.performAction(AccessibilityNodeInfo.ACTION_CLICK)

        // execute gesture if click action doesn't succeed
        if (!success) {
            AppLog.d(TAG, "Click action didn't work. Dispatching gesture")

            val nodeBounds = Rect()
            node.getBoundsInScreen(nodeBounds)
            val tapPath = Path()
            tapPath.moveTo(nodeBounds.centerX().toFloat(), nodeBounds.centerY().toFloat())
            val tapStroke =
                StrokeDescription(tapPath, 0, ViewConfiguration.getTapTimeout().toLong())

            val gestureBuilder = GestureDescription.Builder()
            gestureBuilder.addStroke(tapStroke)
            dispatchGesture(gestureBuilder.build(), null, null)
        }
    }

    private fun toggleMute(mute: Boolean) {
        val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        val isCurrentlyMuted = audioManager.isStreamMute(AudioManager.STREAM_MUSIC)
        if (isCurrentlyMuted != mute) {
            audioManager.adjustStreamVolume(
                AudioManager.STREAM_MUSIC,
                if (mute) AudioManager.ADJUST_MUTE else AudioManager.ADJUST_UNMUTE, 0
            )
        }
        if (mute) {
            AppLog.d(TAG, "Toggling mute to true")
        } else {
            AppLog.d(TAG, "Toggling mute to false")
        }
    }

    override fun onUnbind(intent: Intent?): Boolean {
        toggleMute(false)
        return true
    }

    override fun onInterrupt() {
        toggleMute(false)
    }

    override fun onDestroy() {
        super.onDestroy()

        if (configReader != null) {
            try {
                configReader.close()
            } catch (e: Exception) {
                e(TAG, e.message!!, e)
            }
        }
    }

    public override fun onServiceConnected() {
        enable(null) // enable ordinary logging

        val prefs = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        muteAds = prefs.getBoolean(MUTE_ADS_PREF, true)
        val listener: OnSharedPreferenceChangeListener?
        prefs.registerOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener { p: SharedPreferences?, key: String? ->
            AppLog.d(TAG, "onServiceConnected: pref changed {0}", key)
            if (key == MUTE_ADS_PREF) {
                muteAds = p!!.getBoolean(key, false)
                AppLog.d(TAG, "onServiceConnected: muteAds now {0}", muteAds)
            } else if (key == CAPTURE_LOGS_PREF) {
                captureLogs = p!!.getBoolean(key, false)
                if (!captureLogs) {
                    disable()
                    enable(null) // enable ordinary logging
                } else {
                    enable(applicationContext)
                }
                AppLog.d(TAG, "onServiceConnected: captureLogs now {0}", captureLogs)
            }
        }.also { listener = it })
        listenerRef.set(listener)

        val rulesParser = RulesParser()
        packageClickRules.set(
            mutableMapOf(
                "com.google.android.youtube" to
                    rulesParser.orRules(
                        *listOf(
                            "skip_ad_button",
                            "modern_miniplayer_skip_ad_button"
                        ).map
                        { rulesParser.parse(it, "com.google.android.youtube") }
                            .toTypedArray()),
                "com.google.android.apps.youtube.music" to
                    rulesParser.orRules(
                        *listOf(
                            "skip_ad_button",
                            "snackbar_action&_ruleid_no_recent_user_click,10000"
                        ).map
                        {
                            rulesParser.parse(
                                it,
                                "com.google.android.apps.youtube.music"
                            )
                        }.toTypedArray())
            )
        )

        packageMuteRules.set(
            mutableMapOf(
                "com.google.android.youtube" to
                    rulesParser.orRules(
                        *listOf(
                            "player_learn_more_button",
                            "ad_progress_text",
                            "modern_miniplayer_ad_badge",
                            "ad_badge&!collapsible_ad_cta_overlay_container"
                        ).map
                        { rulesParser.parse(it, "com.google.android.youtube") }
                            .toTypedArray()),
                "com.google.android.apps.youtube.music" to
                    rulesParser.orRules(
                        *listOf(
                            "player_learn_more_button",
                            "ad_progress_text"
                        ).map
                        {
                            rulesParser.parse(
                                it,
                                "com.google.android.apps.youtube.music"
                            )
                        }.toTypedArray())
            )
        )

        /*
        configReader = new ConfigReader(config -> {
            packageClickRules.set(config.getClickRules().entrySet().stream().collect(Collectors.toMap(
                    Map.Entry::getKey,
                    entry -> rulesParser.orRules(
                            entry.getValue().stream().map(s -> rulesParser.parse(s, entry.getKey())).toArray(BaseRule[]::new))
            )));
            packageMuteRules.set(config.getMuteRules().entrySet().stream().collect(Collectors.toMap(
                    Map.Entry::getKey,
                    entry -> rulesParser.orRules(
                            entry.getValue().stream().map(s -> rulesParser.parse(s, entry.getKey())).toArray(BaseRule[]::new))
            )));

            AccessibilityServiceInfo serviceInfo = getServiceInfo();
            Set<String> packages = new HashSet<>(config.getClickRules().keySet());
            packages.addAll(config.getMuteRules().keySet());
            serviceInfo.packageNames = packages.toArray(new String[0]);
            setServiceInfo(serviceInfo);

            AppLog.d(TAG, "Click rules are: {0}", packageClickRules.get());
            AppLog.d(TAG, "Mute rules are: {0}", packageMuteRules.get());
            AppLog.d(TAG, "Packages are: {0}", Arrays.asList(serviceInfo.packageNames));
        });*/
    }

    companion object {
        const val MUTE_ADS_PREF: String = "mute_ads"

        const val CAPTURE_LOGS_PREF: String = "enable_logging"

        private val TAG: String = AdSkipperService::class.java.getName()
    }
}
