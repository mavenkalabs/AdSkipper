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
import com.mavenkalabs.adskipper.databinding.FragmentServiceDisabledBinding
import com.mavenkalabs.adskipper.service.AdSkipperService
import java.util.Arrays

class ServiceDisabledFragment : Fragment() {
    private var binding: FragmentServiceDisabledBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentServiceDisabledBinding.inflate(inflater, container, false)
        return binding!!.getRoot()
    }

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

        binding!!.buttonAgree.setOnClickListener { v: View? ->
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            startActivity(intent)
        }

        binding!!.buttonDisagree.setOnClickListener { v: View? -> requireActivity().finishAndRemoveTask() }

        binding!!.buttonTutorial.setOnClickListener { v: View? ->
            TutorialFragment().show(
                getChildFragmentManager(), TutorialFragment.TAG
            )
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
                        /* resolver = */ requireContext().applicationContext.contentResolver,
                        /* name = */ Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
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