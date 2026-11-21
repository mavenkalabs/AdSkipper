package com.mavenkalabs.adskipper

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.provider.Settings.SettingNotFoundException
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentContainerView
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.withStarted
import androidx.navigation.fragment.NavHostFragment
import androidx.preference.PreferenceManager
import com.mavenkalabs.adskipper.engage.UsageDataManager
import com.mavenkalabs.adskipper.model.MainViewModel
import com.mavenkalabs.adskipper.service.AdSkipperService
import com.mavenkalabs.adskipper.util.AppLog
import com.mavenkalabs.adskipper.util.ProUtils
import com.revenuecat.purchases.models.StoreProduct
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Arrays
import kotlin.time.Duration.Companion.days

class ServiceEnabledFragment : Fragment(R.layout.fragment_service_enabled) {
    companion object {
        private val TAG = ServiceEnabledFragment::class.java.name
    }
    private lateinit var loadingSpinner: ProgressBar
    private lateinit var premiumChildFragment: FragmentContainerView
    private lateinit var statsChildFragment: FragmentContainerView
    private lateinit var reqNotifChildFragment: FragmentContainerView

    private val mainViewModel: MainViewModel by activityViewModels()

    private var showPremiumEnabledFragment = false

    override fun onResume() {
        super.onResume()

        if (!this.isA11yServiceEnabled) {
            val navController = NavHostFragment.findNavController(this)
            navController.popBackStack()
            navController.navigate(R.id.ServiceDisabledFragment)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loadingSpinner = view.findViewById(R.id.premium_status_spinner)
        premiumChildFragment = view.findViewById(R.id.premium_fragment_container)
        statsChildFragment = view.findViewById(R.id.stats_fragment_container)
        reqNotifChildFragment = view.findViewById(R.id.req_notif_fragment_container)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                mainViewModel.requestNotifications.collect { connected ->
                    if (connected) {
                        // Mark event as handled so rotation doesn't re-trigger it
                        mainViewModel.consumeRequestNotificationsEvent()

                        // Show your rationale card/dialog in the Fragment
                        showNotificationRationaleUI()
                        childFragmentManager.setFragmentResultListener("refreshNotificationStatus", viewLifecycleOwner) { _, result ->
                            if (result.getBoolean("result")) {
                                childFragmentManager.findFragmentById(R.id.req_notif_fragment_container)
                                    ?.let {
                                        childFragmentManager.beginTransaction().remove(it).commit()
                                    }
                                reqNotifChildFragment.visibility = View.GONE
                            }
                        }
                    }
                }
            }
        }

        val usageDataManager = UsageDataManager(requireContext())
        val isStatUseful = (usageDataManager.getStatAsInt(UsageDataManager.USAGE_DATA_KEY_SKIP_COUNT_S1)?:0) > 0
        if (isStatUseful) {
            childFragmentManager.beginTransaction()
                .setReorderingAllowed(true)
                .replace(R.id.stats_fragment_container, StatsFragment())
                .commit()
            statsChildFragment.visibility = View.VISIBLE
        } else {
            childFragmentManager.findFragmentById(R.id.stats_fragment_container)?.let {
                childFragmentManager.beginTransaction().remove(it).commit()
            }
            statsChildFragment.visibility = View.GONE
        }

        childFragmentManager.setFragmentResultListener("refreshPremiumStatus", viewLifecycleOwner) { _, _ ->
            showPremiumEnabledFragment = true
            checkAndShowPremium()
        }

        val buttonGotoA11ySettings = view.findViewById<Button>(R.id.button_goto_a11y_settings)
        buttonGotoA11ySettings?.setOnClickListener { _: View? ->
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            startActivity(intent)
        }

        val prefs = PreferenceManager.getDefaultSharedPreferences(requireContext())
        if (!prefs.contains(getString(R.string.pref_key_mute_ads))) {
            prefs.edit { putBoolean(getString(R.string.pref_key_mute_ads), true) }
        }

        checkAndShowPremium()
    }

    private fun showNotificationRationaleUI() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return

        val isGranted = ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED

        if (!isGranted) {
            childFragmentManager.beginTransaction()
                .setReorderingAllowed(true)
                .replace(R.id.req_notif_fragment_container, RequestNotificationsFragment())
                .commit()
            reqNotifChildFragment.visibility = View.VISIBLE
        } else {
            childFragmentManager.findFragmentById(R.id.req_notif_fragment_container)?.let {
                childFragmentManager.beginTransaction().remove(it).commit()
            }
            reqNotifChildFragment.visibility = View.GONE
        }
    }
    private fun checkAndShowPremium() {
        loadingSpinner.visibility = View.VISIBLE
        premiumChildFragment.visibility = View.GONE

        if (hasAppUsage()) {
            viewLifecycleOwner.lifecycleScope.launch {
                var products: List<StoreProduct> = emptyList()
                try {
                    products = withContext(Dispatchers.IO) {
                        try {
                            ProUtils.getProducts()
                        } catch (e: Exception) {
                            AppLog.e(TAG, e.message ?: e.toString(), e)

                            return@withContext emptyList()
                        }
                    }

                    withStarted {
                        if (products.isNotEmpty()) {
                            if (showPremiumEnabledFragment) {
                                childFragmentManager.beginTransaction()
                                    .setReorderingAllowed(true)
                                    .replace(R.id.premium_fragment_container, PremiumEnabledFragment())
                                    .commit()
                            } else {
                                childFragmentManager.beginTransaction()
                                    .setReorderingAllowed(true)
                                    .replace(R.id.premium_fragment_container, PremiumDisabledFragment())
                                    .commit()
                            }
                        } else {
                            val existingChild =
                                childFragmentManager.findFragmentById(R.id.premium_fragment_container)
                            if (existingChild != null) {
                                childFragmentManager.beginTransaction().remove(existingChild).commit()
                            }
                        }
                    }
                } finally {
                    loadingSpinner.visibility = View.GONE
                    if (products.isNotEmpty()) {
                        premiumChildFragment.visibility = View.VISIBLE
                    }
                }
            }
        } else {
            loadingSpinner.visibility = View.GONE
        }
    }

    fun hasAppUsage(): Boolean {
        val usageThreshold = 30.days.inWholeMilliseconds

        return try {
            val result = context?.let {
                val packageInfo = it.packageManager.getPackageInfo(it.packageName, 0)
                packageInfo.firstInstallTime < (System.currentTimeMillis() - usageThreshold)
            }

            result?:false
        } catch (_: PackageManager.NameNotFoundException) {
            false
        }
    }

    private val isA11yServiceEnabled: Boolean
        get() {
            val service =
                "${requireContext().packageName}/${AdSkipperService::class.java.getCanonicalName()}"
            try {
                val accessibilityEnabled = Settings.Secure.getInt(
                    requireContext().applicationContext.contentResolver,
                    Settings.Secure.ACCESSIBILITY_ENABLED
                )
                if (accessibilityEnabled == 1) {
                    val settingValue = Settings.Secure.getString(
                        requireContext().applicationContext.contentResolver,
                        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
                    )
                    if (settingValue != null) {
                        return Arrays.stream(
                            settingValue.split(":".toRegex()).dropLastWhile { it.isEmpty() }
                                .toTypedArray())
                            .anyMatch { s: String? -> s.equals(service, ignoreCase = true) }
                    }
                }

                return false
            } catch (_: SettingNotFoundException) {
                return false
            }
        }
}