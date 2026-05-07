package com.mavenkalabs.adskipper.util;

import androidx.annotation.VisibleForTesting;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class ConfigReader implements AutoCloseable {
    private static final String CONFIG_URL = "https://mavenkalabs.github.io/pubdocs/adskipper/config.json";
    private static final String HTTP_RES_HEADER_ETAG = "Etag";
    private static final String HTTP_REQ_HEADER_ETAG = "If-None-Match";
    private static final String HTTP_REQ_METHOD = "GET";
    private static final int HTTP_RES_STATUS_OK = 200;
    private static final String TAG = ConfigReader.class.getName();

    private final ScheduledExecutorService executorService;
    private final Consumer<Config> callback;

    private String version;

    public ConfigReader(Consumer<Config> callback) {
        this.callback = callback;
        this.executorService = Executors.newSingleThreadScheduledExecutor();
        this.executorService.scheduleWithFixedDelay(this::loadConfig, 0, 1, TimeUnit.DAYS);
    }

    @VisibleForTesting
    public void loadConfig() {
        try {
            URL url = new URL(CONFIG_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod(HTTP_REQ_METHOD);
            if (version != null) {
                conn.setRequestProperty(HTTP_REQ_HEADER_ETAG, version);
            }
            if (conn.getResponseCode() == HTTP_RES_STATUS_OK) {
                version = conn.getHeaderField(HTTP_RES_HEADER_ETAG);
                try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                    JSONObject jsonObject = new JSONObject(br.lines().collect(Collectors.joining()));

                    Config config = new Config();
                    config.version = jsonObject.getString("version");
                    config.clickRules = new HashMap<>();
                    config.muteRules = new HashMap<>();
                    JSONArray packagesArray = jsonObject.getJSONArray("packages");
                    for (int i = packagesArray.length() - 1; i >= 0; i--) {
                        JSONObject packageObject = packagesArray.getJSONObject(i);
                        JSONArray clickRulesArr = packageObject.getJSONArray("click_rules");
                        ArrayList<String> clickRules = new ArrayList<>(clickRulesArr.length());
                        for (int j = clickRulesArr.length() - 1; j >= 0; j--) {
                            clickRules.add(clickRulesArr.getString(j));
                        }
                        JSONArray muteRulesArr = packageObject.getJSONArray("mute_rules");
                        ArrayList<String> muteRules = new ArrayList<>(muteRulesArr.length());
                        for (int j = muteRulesArr.length() - 1; j >= 0; j--) {
                            muteRules.add(muteRulesArr.getString(j));
                        }

                        config.clickRules.put(packageObject.getString("package"), clickRules);
                        config.muteRules.put(packageObject.getString("package"), muteRules);
                    }

                    callback.accept(config);
                }
                conn.disconnect();
            }
        } catch (Throwable t) {
            // ignore
            // no callback happens
            AppLog.e(TAG, t.getMessage(), t);
        }
    }

    @Override
    public void close() {
        executorService.close();
    }

    public static class Config {
        public String version;

        Map<String, List<String>> clickRules;
        Map<String, List<String>> muteRules;

        public Map<String, List<String>> getClickRules() {
            return Collections.unmodifiableMap(clickRules);
        }

        public Map<String, List<String>> getMuteRules() {
            return Collections.unmodifiableMap(muteRules);
        }
    }
}
