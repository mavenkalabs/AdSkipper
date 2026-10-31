package com.mavenkalabs.adskipper

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.provider.Settings.SettingNotFoundException
import android.view.View
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.NavHostFragment
import com.mavenkalabs.adskipper.service.AdSkipperService
import java.util.Arrays

class ServiceDisabledFragment : Fragment(R.layout.fragment_service_disabled) {
    override fun onResume() {
        super.onResume()

        if (this.isA11yServiceEnabled) {
            val navController = NavHostFragment.findNavController(this)
            navController.popBackStack()
            navController.navigate(R.id.ServiceEnabledFragment)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val buttonAgree = view.findViewById<Button>(R.id.button_agree)
        val buttonDisagree = view.findViewById<Button>(R.id.button_disagree)
        val buttonTutorial = view.findViewById<Button>(R.id.button_tutorial)

        buttonAgree?.setOnClickListener { _: View? ->
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            startActivity(intent)
        }

        buttonDisagree?.setOnClickListener { _: View? -> requireActivity().finishAndRemoveTask() }

        buttonTutorial?.setOnClickListener { _: View? ->
            TutorialFragment().show(
                getChildFragmentManager(), TutorialFragment.TAG
            )
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
                        /* resolver = */ requireContext().applicationContext.contentResolver,
                        /* name = */ Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
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