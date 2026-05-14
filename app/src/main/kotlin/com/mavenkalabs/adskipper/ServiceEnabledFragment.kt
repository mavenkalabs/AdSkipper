package com.mavenkalabs.adskipper

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.provider.Settings.SettingNotFoundException
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.NavHostFragment
import androidx.preference.PreferenceManager
import com.mavenkalabs.adskipper.databinding.FragmentServiceEnabledBinding
import com.mavenkalabs.adskipper.service.AdSkipperService
import java.util.Arrays
import androidx.core.content.edit

class ServiceEnabledFragment : Fragment() {
    private var binding: FragmentServiceEnabledBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentServiceEnabledBinding.inflate(inflater, container, false)
        return binding?.getRoot()
    }

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

        binding?.buttonGotoA11ySettings?.setOnClickListener { v: View? ->
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            startActivity(intent)
        }

        val prefs = PreferenceManager.getDefaultSharedPreferences(requireContext())
        if (!prefs.contains(AdSkipperService.MUTE_ADS_PREF)) {
            prefs.edit { putBoolean(AdSkipperService.MUTE_ADS_PREF, true) }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
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
                        return Arrays.stream<String?>(
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