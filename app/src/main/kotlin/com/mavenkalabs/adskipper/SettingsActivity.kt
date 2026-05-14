package com.mavenkalabs.adskipper

import android.os.Build
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.mavenkalabs.adskipper.service.AdSkipperService

class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_activity)
        setSupportActionBar(findViewById<Toolbar?>(R.id.settings_toolbar))
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
                val preference = findPreference<Preference?>(AdSkipperService.CAPTURE_LOGS_PREF)
                if (preference != null) {
                    preferenceScreen.removePreference(preference)
                }
            }
        }
    }
}