package com.mavenkalabs.adskipper.service;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Path;
import android.graphics.Rect;
import android.media.AudioManager;
import android.view.ViewConfiguration;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import androidx.preference.PreferenceManager;

import com.mavenkalabs.adskipper.rules.BaseRule;
import com.mavenkalabs.adskipper.rules.RuleConstants;
import com.mavenkalabs.adskipper.rules.RuleResult;
import com.mavenkalabs.adskipper.rules.RulesParser;
import com.mavenkalabs.adskipper.util.AppLog;
import com.mavenkalabs.adskipper.util.ConfigReader;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

public class AdSkipperService extends AccessibilityService  {
    public static final String MUTE_ADS_PREF = "mute_ads";

    public static final String CAPTURE_LOGS_PREF = "enable_logging";

    private boolean muteAds = false;

    private boolean captureLogs = false;

    private boolean adInProgress = false;

    private ConfigReader configReader;

    private long lastClickTS = 0L;

    private final AtomicReference<SharedPreferences.OnSharedPreferenceChangeListener> listenerRef = new AtomicReference<>();

    private final AtomicReference<Map<String, BaseRule>> packageClickRules = new AtomicReference<>();

    private final AtomicReference<Map<String, BaseRule>> packageMuteRules = new AtomicReference<>();

    private static final String TAG = AdSkipperService.class.getName();

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        try {
            if (event.getEventType() == AccessibilityEvent.TYPE_VIEW_CLICKED) {
                lastClickTS = System.currentTimeMillis();
                return;
            }

            Map<String, Object> parameters = Collections.singletonMap(RuleConstants.RULE_PARAM_LAST_USER_CLICK_TS, lastClickTS);

            final String eventPkgName = (event.getPackageName() != null ? event.getPackageName().toString() : null);
            final AccessibilityNodeInfo rootNode = getRootInActiveWindow();
            BaseRule currentMuteRules = packageMuteRules.get().get(eventPkgName);
            if (rootNode != null && currentMuteRules != null) {
                boolean conditionSatisfied = currentMuteRules.apply(rootNode, parameters).isPassed();
                if (conditionSatisfied) {
                    if (muteAds && !adInProgress) {
                        toggleMute(true);
                        adInProgress = true;
                        AppLog.d(TAG, "onAccessibilityEvent: Detected ad");
                        if (captureLogs) {
                            AppLog.logAccessibilityEvent(getRootInActiveWindow(), AppLog.EventType.AD);
                        }
                    }
                }

                if (muteAds && !conditionSatisfied) {
                    if (adInProgress) {
                        toggleMute(false);
                        adInProgress = false;
                    }
                }
            }

            BaseRule currentClickRules = packageClickRules.get().get(eventPkgName);
            if (rootNode != null && currentClickRules != null) {
                RuleResult result = currentClickRules.apply(rootNode, parameters);

                boolean conditionSatisfied =  result.isPassed();
                List<AccessibilityNodeInfo> foundNodes = result.getFilteredNodes();
                if (conditionSatisfied && foundNodes != null && !foundNodes.isEmpty()) {
                    foundNodes.stream()
                            .filter(AccessibilityNodeInfo::isClickable)
                            .findFirst()
                            .ifPresent(accessibilityNodeInfo -> {
                                tap(accessibilityNodeInfo);
                                AppLog.d(TAG, "onAccessibilityEvent: Skipped ad");
                                if (captureLogs) {
                                    AppLog.logAccessibilityEvent(getRootInActiveWindow(), AppLog.EventType.SKIP);
                                }
                            });
                }

            }
        } catch (Exception e) {
            AppLog.e(TAG, "Unexpected error", e);
        }
    }

    private void tap(AccessibilityNodeInfo node) {
        //try click action first
        boolean success = node.performAction(AccessibilityNodeInfo.ACTION_CLICK);

        // execute gesture if click action doesn't succeed
        if (!success) {
            Rect nodeBounds = new Rect();
            node.getBoundsInScreen(nodeBounds);
            Path tapPath = new Path();
            tapPath.moveTo(nodeBounds.centerX(), nodeBounds.centerY());
            GestureDescription.StrokeDescription tapStroke =
                    new GestureDescription.StrokeDescription(tapPath, 0, ViewConfiguration.getTapTimeout());

            GestureDescription.Builder gestureBuilder = new GestureDescription.Builder();
            gestureBuilder.addStroke(tapStroke);
            dispatchGesture(gestureBuilder.build(), null, null);
        }
    }

    private void toggleMute(boolean mute) {
        AudioManager audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        boolean isCurrentlyMuted = audioManager.isStreamMute(AudioManager.STREAM_MUSIC);
        if (isCurrentlyMuted != mute) {
            audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC,
                    mute ? AudioManager.ADJUST_MUTE : AudioManager.ADJUST_UNMUTE, 0);
        }
        if (mute) {
            AppLog.d(TAG, "Toggling mute to true");
        } else {
            AppLog.d(TAG, "Toggling mute to false");
        }
    }

    @Override
    public boolean onUnbind(Intent intent) {
        toggleMute(false);
        return true;
    }

    @Override
    public void onInterrupt() {
        toggleMute(false);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (configReader != null) {
            try {
                configReader.close();
            } catch (Exception e) {
                AppLog.e(TAG, e.getMessage(), e);
            }
        }
    }

    @Override
    protected void onServiceConnected() {
        AppLog.enable(null); // enable ordinary logging

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        muteAds = prefs.getBoolean(MUTE_ADS_PREF, true);
        SharedPreferences.OnSharedPreferenceChangeListener listener;
        prefs.registerOnSharedPreferenceChangeListener(listener = (p, key) -> {
            AppLog.d(TAG, "onServiceConnected: pref changed {0}", key);

            if (Objects.equals(key, MUTE_ADS_PREF)) {
                muteAds = p.getBoolean(key, false);
                AppLog.d(TAG, "onServiceConnected: muteAds now {0}", muteAds);
            } else if (Objects.equals(key, CAPTURE_LOGS_PREF)) {
                captureLogs = p.getBoolean(key, false);
                if (!captureLogs) {
                    AppLog.disable();
                    AppLog.enable(null); // enable ordinary logging
                } else {
                    AppLog.enable(getApplicationContext());
                }
                AppLog.d(TAG, "onServiceConnected: captureLogs now {0}", captureLogs);
            }
        });
        listenerRef.set(listener);

        final RulesParser rulesParser = new RulesParser();
        packageClickRules.set(Map.of(
        "com.google.android.youtube",
                rulesParser.orRules(Stream.of("skip_ad_button", "modern_miniplayer_skip_ad_button").
                        map(s -> rulesParser.parse(s, "com.google.android.youtube")).toArray(BaseRule[]::new)),
        "com.google.android.apps.youtube.music",
                rulesParser.orRules(Stream.of("skip_ad_button", "snackbar_action&_ruleid_no_recent_user_click,10000").
                        map(s -> rulesParser.parse(s, "com.google.android.apps.youtube.music")).toArray(BaseRule[]::new))
        ));

        packageMuteRules.set(Map.of(
        "com.google.android.youtube",
                rulesParser.orRules(Stream.of("player_learn_more_button",  "ad_progress_text",
                                "modern_miniplayer_ad_badge", "ad_badge&!collapsible_ad_cta_overlay_container").
                        map(s -> rulesParser.parse(s, "com.google.android.youtube")).toArray(BaseRule[]::new)),
        "com.google.android.apps.youtube.music",
                rulesParser.orRules(Stream.of("player_learn_more_button",  "ad_progress_text").
                        map(s -> rulesParser.parse(s, "com.google.android.apps.youtube.music")).toArray(BaseRule[]::new))
        ));

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
}
