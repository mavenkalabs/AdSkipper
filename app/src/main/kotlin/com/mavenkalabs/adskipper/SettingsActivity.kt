package com.mavenkalabs.adskipper

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import com.mavenkalabs.adskipper.util.AppLog
import com.mavenkalabs.adskipper.util.ProUtils

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
        private val requestAudioPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            val voiceCommandSwitch = findPreference<SwitchPreferenceCompat>(getString(R.string.pref_key_pro_enable_voice))
            if (isGranted) {
                voiceCommandSwitch?.isChecked = true
            } else {
                voiceCommandSwitch?.isChecked = false
                Toast.makeText(requireContext(), R.string.error_voice_permission, Toast.LENGTH_SHORT).show()
            }
        }

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.preferences, rootKey)

            // not available for old devices since the log capture needs permission request etc
            handleLogCapturePrefs()

            handleProPrefs()
        }

        private fun handleProPrefs() {
            val proSettings = findPreference<PreferenceCategory>(getString(R.string.pref_key_pro_settings))
            if (proSettings != null) {
                if (ProUtils.isPro()) {
                    proSettings.isVisible = true

                    val voiceCommandSwitch = findPreference<SwitchPreferenceCompat>(getString(R.string.pref_key_pro_enable_voice))

                    voiceCommandSwitch?.onPreferenceChangeListener =
                        Preference.OnPreferenceChangeListener { _, newValue ->
                            val isChecked = newValue as Boolean

                            if (!isChecked) {
                                return@OnPreferenceChangeListener true
                            }

                            val isGranted = ContextCompat.checkSelfPermission(
                                requireContext(),
                                Manifest.permission.RECORD_AUDIO
                            ) == PackageManager.PERMISSION_GRANTED

                            if (isGranted) {
                                return@OnPreferenceChangeListener true
                            } else {
                                requestAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                return@OnPreferenceChangeListener false
                            }
                        }
                } else {
                    preferenceScreen.removePreferenceRecursively(getString(R.string.pref_key_pro_settings))
                }
            }
        }

        private fun handleLogCapturePrefs() {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                var preference = findPreference<Preference?>(getString(R.string.pref_key_enable_logging))
                if (preference != null) {
                    preferenceScreen.removePreference(preference)
                }
                preference = findPreference(getString(R.string.pref_key_dump_logs))
                if (preference != null) {
                    preferenceScreen.removePreference(preference)
                }
            } else {
                val captureLogsPref =
                    findPreference<SwitchPreferenceCompat>(getString(R.string.pref_key_enable_logging))
                val saveLogsPref = findPreference<Preference>(getString(R.string.pref_key_dump_logs))

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
                        Toast.makeText(context, R.string.msg_dump_logs_success, Toast.LENGTH_SHORT)
                            .show()
                        true
                    }
            }
        }
    }
}