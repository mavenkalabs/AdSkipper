package com.mavenkalabs.adskipper

import android.os.Build
import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import com.mavenkalabs.adskipper.service.AdSkipperService
import com.mavenkalabs.adskipper.util.AppLog

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
        }
    }
}