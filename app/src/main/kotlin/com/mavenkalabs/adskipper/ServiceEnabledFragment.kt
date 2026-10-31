package com.mavenkalabs.adskipper

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.provider.Settings.SettingNotFoundException
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import androidx.core.content.edit
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentContainerView
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import androidx.preference.PreferenceManager
import com.mavenkalabs.adskipper.service.AdSkipperService
import com.mavenkalabs.adskipper.util.AppLog
import com.mavenkalabs.adskipper.util.ProUtils
import com.revenuecat.purchases.models.StoreProduct
import kotlinx.coroutines.launch
import java.util.Arrays

class ServiceEnabledFragment : Fragment(R.layout.fragment_service_enabled) {
    companion object {
        private val TAG = ServiceEnabledFragment::class.java.name
    }
    private lateinit var loadingSpinner: ProgressBar
    private lateinit var childFragment: FragmentContainerView

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
        childFragment = view.findViewById(R.id.premium_fragment_container)

        childFragmentManager.setFragmentResultListener("refreshPremiumStatus", this) { _, _ ->
            showPremiumEnabledFragment = true
            checkPremiumStatus()
        }

        val buttonGotoA11ySettings = view.findViewById<Button>(R.id.button_goto_a11y_settings)
        buttonGotoA11ySettings?.setOnClickListener { _: View? ->
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            startActivity(intent)
        }

        val prefs = PreferenceManager.getDefaultSharedPreferences(requireContext())
        if (!prefs.contains(AdSkipperService.MUTE_ADS_PREF)) {
            prefs.edit { putBoolean(AdSkipperService.MUTE_ADS_PREF, true) }
        }

        checkPremiumStatus()
    }

    private fun checkPremiumStatus() {
        loadingSpinner.visibility = View.VISIBLE
        childFragment.visibility = View.GONE

        viewLifecycleOwner.lifecycleScope.launch {
            var products : List<StoreProduct> = emptyList()
            try {
                products = ProUtils.getProducts()
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
                }
            } catch (e: Exception) {
                AppLog.e(TAG, e.message ?: e.toString(), e)

                val existingChild = childFragmentManager.findFragmentById(R.id.premium_fragment_container)
                if (existingChild != null) {
                    childFragmentManager.beginTransaction().remove(existingChild).commit()
                }
            } finally {
                loadingSpinner.visibility = View.GONE
                if (products.isNotEmpty()) {
                    childFragment.visibility = View.VISIBLE
                }
            }
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