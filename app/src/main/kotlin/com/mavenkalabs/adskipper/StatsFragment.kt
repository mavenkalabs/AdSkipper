package com.mavenkalabs.adskipper

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.mavenkalabs.adskipper.engage.UsageDataManager

class StatsFragment : Fragment(R.layout.fragment_stats) {
    private lateinit var thisCycleStatsTextView: TextView
    private lateinit var pastCycleStatsTextView: TextView
    private lateinit var lifetimeStatsTextView: TextView

    override fun onResume() {
        super.onResume()

        populateStats()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        thisCycleStatsTextView = view.findViewById(R.id.stats_this_cycle)
        pastCycleStatsTextView = view.findViewById(R.id.stats_past_cycle)
        lifetimeStatsTextView = view.findViewById(R.id.stats_lifetime)

        populateStats()
    }

    private fun populateStats() {
        val usageDataManager = UsageDataManager(requireContext())

        val adCountLive = usageDataManager.getStatAsInt(UsageDataManager.USAGE_DATA_KEY_AD_COUNT_LIVE)?:0
        val skipCountLive = usageDataManager.getStatAsInt(UsageDataManager.USAGE_DATA_KEY_SKIP_COUNT_LIVE)?:0

        val adCountPast = usageDataManager.getStatAsInt(UsageDataManager.USAGE_DATA_KEY_AD_COUNT_S1)?:0
        val skipCountPast = usageDataManager.getStatAsInt(UsageDataManager.USAGE_DATA_KEY_SKIP_COUNT_S1)?:0

        val adCountLifeTime = (usageDataManager.getStatAsInt(UsageDataManager.USAGE_DATA_KEY_AD_COUNT_LIFE_TIME)?:0) + adCountLive
        val skipCountLifeTime = (usageDataManager.getStatAsInt(UsageDataManager.USAGE_DATA_KEY_SKIP_COUNT_LIFE_TIME)?:0) + skipCountLive

        thisCycleStatsTextView.text = getString(R.string.msg_engagement_text, skipCountLive, adCountLive)
        pastCycleStatsTextView.text = getString(R.string.msg_engagement_text, skipCountPast, adCountPast)
        lifetimeStatsTextView.text = getString(R.string.msg_engagement_text, skipCountLifeTime, adCountLifeTime)
    }
}