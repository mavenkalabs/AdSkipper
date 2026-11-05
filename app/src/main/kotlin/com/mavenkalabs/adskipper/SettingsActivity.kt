package com.mavenkalabs.adskipper

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.accessibility.AccessibilityManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import com.mavenkalabs.adskipper.service.AdSkipperService
import com.mavenkalabs.adskipper.util.AppLog
import com.mavenkalabs.adskipper.util.ProUtils
import com.mavenkalabs.adskipper.util.VoiceCommandManager
import com.mavenkalabs.adskipper.util.VoiceModelManager
import kotlinx.coroutines.launch

class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_activity)
        setSupportActionBar(findViewById(R.id.settings_toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressedDispatcher.onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    class SettingsFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.preferences, rootKey)

            // not available for old devices since the log capture needs permission request etc
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                var preference = findPreference<Preference?>(AdSkipperService.CAPTURE_LOGS_PREF)
                if (preference != null) {
                    preferenceScreen.removePreference(preference)
                }
                preference = findPreference(AdSkipperService.DUMP_LOGS_PREF)
                if (preference != null) {
                    preferenceScreen.removePreference(preference)
                }
            } else {
                val captureLogsPref =
                    findPreference<SwitchPreferenceCompat>(AdSkipperService.CAPTURE_LOGS_PREF)
                val saveLogsPref = findPreference<Preference>(AdSkipperService.DUMP_LOGS_PREF)

                saveLogsPref?.isVisible = (captureLogsPref?.isChecked == true)

                captureLogsPref?.onPreferenceChangeListener =
                    Preference.OnPreferenceChangeListener { _, newValue ->
                        val isChecked = newValue as Boolean
                        saveLogsPref?.isVisible = isChecked

                        if (isChecked) AppLog.enable() else AppLog.disable()

                        true
                    }

                saveLogsPref?.onPreferenceClickListener =
                    Preference.OnPreferenceClickListener { _ ->
                        AppLog.dump(context)
                        Toast.makeText(context, R.string.dump_logs_success_msg, Toast.LENGTH_SHORT).show()
                        true
                    }
            }

            if (ProUtils.isPro() && isAccessibilityServiceEnabled()) {
                findPreference<PreferenceCategory>("pro_settings")?.isVisible = true
            }

            val voiceCommandsPref = findPreference<SwitchPreferenceCompat>("pref_pro_voice")
            voiceCommandsPref?.onPreferenceChangeListener =
                Preference.OnPreferenceChangeListener { _, newValue ->
                    val isChecked = newValue as Boolean

                    if (isChecked) prepareVoiceInteractionEngine(voiceCommandsPref) else stopVoiceInteractionEngine()

                    true
                }
        }

        private fun stopVoiceInteractionEngine() {
            VoiceCommandManager.stopEngine()
        }

        private fun isAccessibilityServiceEnabled(): Boolean {
            val am = requireContext().getSystemService(ACCESSIBILITY_SERVICE) as AccessibilityManager

            // Get the list of all currently enabled accessibility services
            val enabledServices = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)

            for (enabledService in enabledServices) {
                val serviceInfo = enabledService.resolveInfo.serviceInfo
                // Check if the package name and class name match your service
                if (serviceInfo.packageName == requireContext().packageName && serviceInfo.name == AdSkipperService::class.java.name) {
                    return true
                }
            }
            return false
        }

        private fun prepareVoiceInteractionEngine(voiceCommandsPref: Preference) {
            lifecycleScope.launch {
                var success = false
                val voskModel = VoiceModelManager.getOrFetchModel(requireContext())
                if (voskModel != null) {
                    try {
                        VoiceCommandManager.startEngine(voskModel) { handleIncomingCommand(it) }
                        success = true
                    } catch (_ : SecurityException) {
                        // do nothing
                        success = false
                    }
                } else {
                    Log.e("VoiceEngine", "Unable to load offline speech processing system.")
                }

                if (!success) {
                    voiceCommandsPref.isEnabled = false
                    Toast.makeText(requireContext(), R.string.error_init_voice, Toast.LENGTH_SHORT).show()
                }
            }
        }

        private fun handleIncomingCommand(command: String) {
            Log.d("VoiceEngine", "Executing: $command")
            val action : String = if (command == "go next") {
                "ACTION_TRIGGER_SWIPE_UP"
            } else {
                "ACTION_TRIGGER_SWIPE_DOWN"
            }

            val intent = Intent("${context?.packageName}.ACCESSIBILITY_COMMAND").apply {
                putExtra("EXTRA_COMMAND", action)
            }
            // This broadcasts exclusively inside your own app process
            LocalBroadcastManager.getInstance(requireContext()).sendBroadcast(intent)
        }
    }
}