package com.mavenkalabs.adskipper.engage

import android.content.Context
import androidx.annotation.VisibleForTesting
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import com.mavenkalabs.adskipper.R
import java.util.concurrent.atomic.AtomicReference

object UsageDataManager {
    private val contextRef = AtomicReference<Context?>(null)

    private val usageData: MutableMap<String, AtomicReference<Int>> = mutableMapOf(
        USAGE_DATA_KEY_AD_COUNT_LIFE_TIME to AtomicReference<Int>(0),
        USAGE_DATA_KEY_SKIP_COUNT_LIFE_TIME to AtomicReference<Int>(0),
        USAGE_DATA_KEY_AD_COUNT_LIVE to AtomicReference<Int>(0),
        USAGE_DATA_KEY_SKIP_COUNT_LIVE to AtomicReference<Int>(0),
        USAGE_DATA_KEY_AD_COUNT_S1 to AtomicReference<Int>(0),
        USAGE_DATA_KEY_SKIP_COUNT_S1 to AtomicReference<Int>(0),
    )


    fun init(context: Context) {
        contextRef.compareAndSet(null, run {
            val appContext = context.applicationContext

            val prefs = PreferenceManager.getDefaultSharedPreferences(appContext)
            val usageDataAsString = prefs.getString(appContext.getString(R.string.pref_key_usage_data), "")

            usageDataFromString(usageDataAsString)

            appContext
        })
    }

    @VisibleForTesting
    internal fun reset() {
        contextRef.set(null)
    }

    private fun ensureInit() {
        contextRef.get()?:throw IllegalStateException()
    }

    fun getStatAsInt(key: String) : Int? {
        return usageData[key]?.get()
    }

    fun takeSnapshot() {
        ensureInit()

        val skipCountLive = usageData[USAGE_DATA_KEY_SKIP_COUNT_LIVE]?.getAndSet(0)
        val adCountLive = usageData[USAGE_DATA_KEY_AD_COUNT_LIVE]?.getAndSet(0)

        usageData[USAGE_DATA_KEY_SKIP_COUNT_S1]?.getAndSet(skipCountLive)
        usageData[USAGE_DATA_KEY_AD_COUNT_S1]?.getAndSet(adCountLive)

        save()
    }

    fun logSkip() : Int? {
        ensureInit()

        val newVal = usageData[USAGE_DATA_KEY_SKIP_COUNT_LIVE]?.updateAndGet { it + 1 }
        usageData[USAGE_DATA_KEY_SKIP_COUNT_LIFE_TIME]?.updateAndGet { it + 1 }
        save()

        return newVal
    }

    fun logAd() : Int? {
        ensureInit()

        val newVal = usageData[USAGE_DATA_KEY_AD_COUNT_LIVE]?.updateAndGet { it + 1 }
        usageData[USAGE_DATA_KEY_AD_COUNT_LIFE_TIME]?.updateAndGet { it + 1 }

        save()

        return newVal
    }

    private fun save() {
        ensureInit()

        contextRef.get()?.let {
            PreferenceManager.getDefaultSharedPreferences(it).edit {
                putString(it.getString(R.string.pref_key_usage_data), usageDataToString())
            }
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
                .forEach {
                    usageData[it[0]]?.set(it[1].toIntOrNull()?:0)
                }
        }
    }

    const val USAGE_DATA_KEY_SKIP_COUNT_LIVE = "skipCountLive"
    const val USAGE_DATA_KEY_AD_COUNT_LIVE = "adCountLive"
    const val USAGE_DATA_KEY_SKIP_COUNT_S1 = "skipCountS1"
    const val USAGE_DATA_KEY_AD_COUNT_S1 = "adCountS1"
    const val USAGE_DATA_KEY_SKIP_COUNT_LIFE_TIME = "skipCountLifeTime"
    const val USAGE_DATA_KEY_AD_COUNT_LIFE_TIME = "adCountLifeTime"
}