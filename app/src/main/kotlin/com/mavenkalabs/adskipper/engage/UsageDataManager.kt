package com.mavenkalabs.adskipper.engage

import android.content.Context
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import com.mavenkalabs.adskipper.R
import java.util.concurrent.atomic.AtomicReference

class UsageDataManager (context: Context) {

    private val usageData: MutableMap<String, AtomicReference<Int>> = mutableMapOf(
        USAGE_DATA_KEY_AD_COUNT_LIFE_TIME to AtomicReference<Int>(0),
        USAGE_DATA_KEY_SKIP_COUNT_LIFE_TIME to AtomicReference<Int>(0),
        USAGE_DATA_KEY_AD_COUNT_LIVE to AtomicReference<Int>(0),
        USAGE_DATA_KEY_SKIP_COUNT_LIVE to AtomicReference<Int>(0),
        USAGE_DATA_KEY_AD_COUNT_S1 to AtomicReference<Int>(0),
        USAGE_DATA_KEY_SKIP_COUNT_S1 to AtomicReference<Int>(0),
    )

    init {
        val existingUsageData = PreferenceManager.getDefaultSharedPreferences(context)
            .getString(context.getString(R.string.pref_key_usage_data), null)

        usageDataFromString(existingUsageData)
    }

    fun getStatAsInt(key: String) : Int? {
        return usageData[key]?.get()
    }

    fun takeSnapshot(context: Context) {
        val skipCountLive = usageData[USAGE_DATA_KEY_SKIP_COUNT_LIVE]?.getAndSet(0)
        val adCountLive = usageData[USAGE_DATA_KEY_AD_COUNT_LIVE]?.getAndSet(0)

        usageData[USAGE_DATA_KEY_SKIP_COUNT_S1]?.getAndSet(skipCountLive)
        usageData[USAGE_DATA_KEY_AD_COUNT_S1]?.getAndSet(adCountLive)

        usageData[USAGE_DATA_KEY_SKIP_COUNT_LIFE_TIME]?.accumulateAndGet(
            skipCountLive
        ) { current, incoming -> current + incoming }
        usageData[USAGE_DATA_KEY_AD_COUNT_LIFE_TIME]?.accumulateAndGet(
            adCountLive
        ) { current, incoming -> current + incoming }

        save(context)
    }

    fun logSkip(context: Context) : Int? {
        val newVal = usageData[USAGE_DATA_KEY_SKIP_COUNT_LIVE]?.updateAndGet { it + 1 }
        save(context)

        return newVal
    }

    fun logAd(context: Context) : Int? {
        val newVal = usageData[USAGE_DATA_KEY_AD_COUNT_LIVE]?.updateAndGet { it + 1 }
        save(context)

        return newVal    }

    private fun save(context: Context) {
        PreferenceManager.getDefaultSharedPreferences(context).edit {
            putString(context.getString(R.string.pref_key_usage_data), usageDataToString())
        }
    }

    private fun usageDataToString() : String {
        return usageData.entries.joinToString(", ") { "${it.key}=${it.value.get()}" }
    }

    private fun usageDataFromString(usageDataAsString: String?) {
        usageDataAsString?.let { str ->
            str.split(", ")
                .map { it.split("=", limit = 2) }
                .filter { it.size == 2 }
                .associate { it[0] to (it[1].toIntOrNull()?:0) }
                .entries
                .forEach { e ->
                   usageData[e.key]?.updateAndGet { e.value }
                }
        }
    }

    companion object {
        const val USAGE_DATA_KEY_SKIP_COUNT_LIVE = "skipCountLive"
        const val USAGE_DATA_KEY_AD_COUNT_LIVE = "adCountLive"
        const val USAGE_DATA_KEY_SKIP_COUNT_S1 = "skipCountS1"
        const val USAGE_DATA_KEY_AD_COUNT_S1 = "adCountS1"
        const val USAGE_DATA_KEY_SKIP_COUNT_LIFE_TIME = "skipCountLifeTime"
        const val USAGE_DATA_KEY_AD_COUNT_LIFE_TIME = "adCountLifeTime"
    }
}