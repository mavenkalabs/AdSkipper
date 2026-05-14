package com.mavenkalabs.adskipper.util

import androidx.annotation.VisibleForTesting
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.lang.AutoCloseable
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import java.util.function.Consumer
import java.util.stream.Collectors

class ConfigReader(private val callback: Consumer<Config>) : AutoCloseable {
    private val executorService: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor()

    private var version: String? = null

    init {
        this.executorService.scheduleWithFixedDelay(
            { this.loadConfig() },
            0,
            1,
            TimeUnit.DAYS
        )
    }

    @VisibleForTesting
    fun loadConfig() {
        try {
            val url = URL(CONFIG_URL)
            val conn = url.openConnection() as HttpURLConnection
            conn.setRequestMethod(HTTP_REQ_METHOD)
            if (version != null) {
                conn.setRequestProperty(HTTP_REQ_HEADER_ETAG, version)
            }
            if (conn.getResponseCode() == HTTP_RES_STATUS_OK) {
                version = conn.getHeaderField(HTTP_RES_HEADER_ETAG)
                BufferedReader(InputStreamReader(conn.getInputStream())).use { br ->
                    val jsonObject = JSONObject(br.lines().collect(Collectors.joining()))
                    val config = Config()
                    config.version = jsonObject.getString("version")
                    config.clickRules = mutableMapOf()
                    config.muteRules = mutableMapOf()
                    val packagesArray = jsonObject.getJSONArray("packages")
                    for (i in packagesArray.length() - 1 downTo 0) {
                        val packageObject = packagesArray.getJSONObject(i)
                        val clickRulesArr = packageObject.getJSONArray("click_rules")
                        val clickRules : MutableList<String> = mutableListOf()
                        for (j in clickRulesArr.length() - 1 downTo 0) {
                            clickRules.add(clickRulesArr.getString(j))
                        }
                        val muteRulesArr = packageObject.getJSONArray("mute_rules")
                        val muteRules: MutableList<String> = mutableListOf()
                        for (j in muteRulesArr.length() - 1 downTo 0) {
                            muteRules.add(muteRulesArr.getString(j))
                        }

                        config.clickRules?.put(packageObject.getString("package"), clickRules)
                        config.muteRules?.put(packageObject.getString("package"), muteRules)
                    }
                    callback.accept(config)
                }
                conn.disconnect()
            }
        } catch (t: Throwable) {
            // ignore
            // no callback happens
            AppLog.e(TAG, t.message!!, t)
        }
    }

    override fun close() {
        executorService.shutdown()
    }

    class Config {
        var version: String? = null

        var clickRules: MutableMap<String, List<String>>? = null
        var muteRules: MutableMap<String, List<String>>? = null

    }

    companion object {
        private const val CONFIG_URL = "https://mavenkalabs.github.io/pubdocs/adskipper/config.json"
        private const val HTTP_RES_HEADER_ETAG = "Etag"
        private const val HTTP_REQ_HEADER_ETAG = "If-None-Match"
        private const val HTTP_REQ_METHOD = "GET"
        private const val HTTP_RES_STATUS_OK = 200
        private val TAG: String = ConfigReader::class.java.getName()
    }
}
