package com.mavenkalabs.adskipper

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.widget.Switch
import android.widget.VideoView
import androidx.core.content.edit
import androidx.core.text.HtmlCompat
import androidx.core.text.HtmlCompat.FROM_HTML_MODE_LEGACY
import androidx.preference.PreferenceManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import androidx.test.uiautomator.waitForAppToBeVisible
import com.mavenkalabs.adskipper.engage.UsageDataManager
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertNotNull
import junit.framework.TestCase.assertNull
import junit.framework.TestCase.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Locale

@RunWith(AndroidJUnit4::class)
internal class MainActivityTests {
    private lateinit var uiDevice: UiDevice

    companion object {
        private const val TIMEOUT : Long = 2000
        private const val APP_NAME: String = "Ad Skipper"
        private const val MUTE_ADS_PREF = "mute_ads"
    }

    @Before
    fun setupBefore() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        uiDevice = UiDevice.getInstance(instrumentation)
    }


    @Test
    fun verifyUiAfterDisagree() {
        val activityIntent =
            Intent(ApplicationProvider.getApplicationContext(), MainActivity::class.java)
        activityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        ApplicationProvider.getApplicationContext<Context>().startActivity(activityIntent)

        var found = uiDevice.wait(
            Until.hasObject(
                By.pkg(
                    MainActivity::class.java.getPackage()!!
                        .name
                )
                    .depth(0)
            ), TIMEOUT
        )
        assertTrue(found)

        // verify UI on first launch and then go to Accessibility Settings
        found = uiDevice.hasObject(
            By.text(
                ApplicationProvider.getApplicationContext<Context>()
                    .getString(R.string.label_a11y_service_disabled)
            )
        )
        assertTrue(found)

        found = uiDevice.findObject(
            By.text(
                ApplicationProvider.getApplicationContext<Context>().getString(R.string.label_disagree)
                    .uppercase(Locale.getDefault())
            )
        ).clickAndWait(Until.newWindow(), TIMEOUT)
        assertTrue(found)


        assertNull(
            uiDevice.findObject(
                By.pkg(
                    MainActivity::class.java.getPackage()!!
                        .name
                )
            )
        )
    }

    @Test
    fun verifyUiAfterEnabling() {
        val activityIntent =
            Intent(ApplicationProvider.getApplicationContext(), MainActivity::class.java)
        activityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        ApplicationProvider.getApplicationContext<Context>().startActivity(activityIntent)

        var found = uiDevice.wait(
            Until.hasObject(
                By.pkg(
                    MainActivity::class.java.getPackage()!!
                        .name
                )
                    .depth(0)
            ), TIMEOUT
        )
        assertTrue(found)

        // verify UI on first launch and then go to Accessibility Settings
        found = uiDevice.hasObject(
            By.text(
                ApplicationProvider.getApplicationContext<Context>()
                    .getString(R.string.label_a11y_service_disabled)
            )
        )
        assertTrue(found)

        found = uiDevice.findObject(
            By.text(
                ApplicationProvider.getApplicationContext<Context>().getString(R.string.label_agree)
                    .uppercase(Locale.getDefault())
            )
        ).clickAndWait(Until.newWindow(), TIMEOUT)
        assertTrue(found)

        // toggle the accessibility feature on
        toggleA11ySetting(true)

        // expect the service to be enabled now
        found = uiDevice.wait(
            Until.hasObject(
                By.text(
                    ApplicationProvider.getApplicationContext<Context>()
                        .getString(R.string.label_a11y_service_enabled1)
                )
            ), 10000
        )
        assertTrue(found)

        val prefs =
            PreferenceManager.getDefaultSharedPreferences(
                ApplicationProvider.getApplicationContext())
        assertTrue(prefs.getBoolean(MUTE_ADS_PREF, false))

        launchA11ySettings()
        toggleMuteAdsSetting(false)
        assertFalse(prefs.getBoolean(MUTE_ADS_PREF, true))

        toggleMuteAdsSetting(true)
        assertTrue(prefs.getBoolean(MUTE_ADS_PREF, false))
    }

    @Test
    fun verifyUiTutorial() {
        val activityIntent =
            Intent(ApplicationProvider.getApplicationContext(), MainActivity::class.java)
        activityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        ApplicationProvider.getApplicationContext<Context>().startActivity(activityIntent)

        var found = uiDevice.wait(
            Until.hasObject(
                By.pkg(
                    MainActivity::class.java.getPackage()!!
                        .name
                )
                    .depth(0)
            ), TIMEOUT
        )
        assertTrue(found)

        found = uiDevice.hasObject(
            By.text(
                ApplicationProvider.getApplicationContext<Context>().getString(R.string.label_tutorial)
            )
        )
        assertTrue(found)

        found = uiDevice.findObject(
            By.text(
                ApplicationProvider.getApplicationContext<Context>().getString(R.string.label_tutorial)
            )
        ).clickAndWait(Until.newWindow(), TIMEOUT)
        assertTrue(found)

        found = uiDevice.hasObject(By.clazz(VideoView::class.java))
        assertTrue(found)

        found = uiDevice.wait(Until.gone(By.clazz(VideoView::class.java)), 20000)
        assertTrue(found)
    }

    @Test
    fun verifyUiAfterDisabling() {
        launchA11ySettings()

        toggleA11ySetting(true)

        val activityIntent =
            Intent(ApplicationProvider.getApplicationContext(), MainActivity::class.java)
        activityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        ApplicationProvider.getApplicationContext<Context>().startActivity(activityIntent)

        var found = uiDevice.wait(
            Until.hasObject(
                By.pkg(
                    MainActivity::class.java.getPackage()!!.name
                )
                    .depth(0)
            ), TIMEOUT
        )
        assertTrue(found)

        // verify UI on first launch and then go to Accessibility Settings
        found = uiDevice.hasObject(
            By.text(
                ApplicationProvider.getApplicationContext<Context>()
                    .getString(R.string.label_a11y_service_enabled1)
            )
        )
        assertTrue(found)

        found = uiDevice.findObject(
            By.text(
                ApplicationProvider.getApplicationContext<Context>()
                    .getString(R.string.go_to_a11y_settings).uppercase(Locale.getDefault())
            )
        ).clickAndWait(Until.newWindow(), TIMEOUT)
        assertTrue(found)

        // toggle the accessibility feature off
        toggleA11ySetting(false)

        // expect the service to be enabled now
        found = uiDevice.wait(
            Until.hasObject(
                By.text(
                    ApplicationProvider.getApplicationContext<Context>()
                        .getString(R.string.label_a11y_service_disabled)
                )
            ), 10000
        )
        assertTrue(found)
    }

    @Test
    fun verifyUiWithRequestNotifications() {
        launchA11ySettings()

        toggleA11ySetting(true)

        val activityIntent =
            Intent(ApplicationProvider.getApplicationContext(), MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                putExtra("EXTRA_REQUEST_NOTIFICATIONS", true)
            }

        ApplicationProvider.getApplicationContext<Context>().startActivity(activityIntent)

        // wait for app to become visible
        var found = uiDevice.waitForAppToBeVisible(
            InstrumentationRegistry.getInstrumentation().targetContext.packageName, TIMEOUT)
        assertTrue(found)

        // verify enable notifications button exists
        val requestNotificationsBn = uiDevice.findObject(
            By.text(
                ApplicationProvider.getApplicationContext<Context>()
                    .getString(R.string.label_request_notifications)
            )
        )
        assertNotNull(requestNotificationsBn)

        // tap enable notifications and then allow notifications
        requestNotificationsBn.clickAndWait(Until.newWindow(), TIMEOUT)

        found = uiDevice.findObject(By.text("Allow"))
            .clickAndWait(Until.newWindow(), TIMEOUT)
        assertTrue(found)

        assertFalse(uiDevice.hasObject(By.text(
            ApplicationProvider.getApplicationContext<Context>()
                .getString(R.string.label_request_notifications)
        )))
    }

    @Test
    fun verifyUiWithStats() {
        launchA11ySettings()

        toggleA11ySetting(true)

        val context : Context = ApplicationProvider.getApplicationContext()

        PreferenceManager.getDefaultSharedPreferences(context).edit(commit = true) {
            putString(context.getString(R.string.pref_key_usage_data),
                "adCountLifeTime=6, skipCountLifeTime=6, adCountLive=2, skipCountLive=2, adCountS1=4, skipCountS1=4")
        }

        UsageDataManager.init(context)

        val activityIntent =
            Intent(context, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }

        context.startActivity(activityIntent)

        // wait for app to become visible
        val found = uiDevice.waitForAppToBeVisible(
            context.packageName, TIMEOUT)
        assertTrue(found)

        // verify stats text exists
        assertTrue(uiDevice.hasObject(By.text(
            context.getString(R.string.label_stats_current_prefix)
        )))
        assertTrue(uiDevice.hasObject(By.text(
            context.getString(R.string.label_stats_past_prefix)
        )))
        assertTrue(uiDevice.hasObject(By.text(
            context.getString(R.string.label_stats_lifetime_prefix)
        )))
        assertTrue(uiDevice.hasObject(By.text(
            HtmlCompat.fromHtml(context.getString(R.string.msg_engagement_text, 6, 6),
                FROM_HTML_MODE_LEGACY).toString()
        )))
        assertTrue(uiDevice.hasObject(By.text(
            HtmlCompat.fromHtml(context.getString(R.string.msg_engagement_text, 4, 4),
                FROM_HTML_MODE_LEGACY).toString()
        )))
        assertTrue(uiDevice.hasObject(By.text(
            HtmlCompat.fromHtml(context.getString(R.string.msg_engagement_text, 2, 2),
                FROM_HTML_MODE_LEGACY).toString()
        )))
    }

    private fun launchA11ySettings() {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        InstrumentationRegistry.getInstrumentation().targetContext.startActivity(intent)

        uiDevice.waitForWindowUpdate(null, TIMEOUT)
    }

    private fun toggleMuteAdsSetting(enable: Boolean) {
        // wait for it to show up
        var found = uiDevice.wait(
            Until.hasObject(By.text(APP_NAME)),
            TIMEOUT
        )
        assertTrue(found)

        // open the Ad Skipper switch
        uiDevice.findObject(By.text(APP_NAME)).click()

        // wait for settings to show up
        found =
            uiDevice.wait(Until.hasObject(By.text("Settings")), TIMEOUT)
        assertTrue(found)

        // open the settings activity
        uiDevice.findObject(By.text("Settings")).click()

        // wait for mute ads switch to show up
        found = uiDevice.wait(
            Until.hasObject(
                By.text(
                    ApplicationProvider.getApplicationContext<Context>()
                        .getString(R.string.label_mute_ads)
                )
            ), TIMEOUT
        )
        assertTrue(found)

        // check if toggle state is different from desired state
        // the first switch is the mute ads one
        var toggleButton = uiDevice.findObject(By.clazz(Switch::class.java))
        val currentState = toggleButton.isChecked
        if (currentState != enable) {
            toggleButton.click()
            if (enable) {
                toggleButton = uiDevice.findObject(By.clazz(Switch::class.java))
                assertTrue(toggleButton.isChecked)
            } else {
                toggleButton = uiDevice.findObject(By.clazz(Switch::class.java))
                assertFalse(toggleButton.isChecked)
            }
        }
        uiDevice.pressBack()
        uiDevice.waitForWindowUpdate(null, TIMEOUT)
        uiDevice.pressBack()
        uiDevice.waitForWindowUpdate(null, TIMEOUT)
    }

    private fun toggleA11ySetting(enable: Boolean) {
        // wait for it to show up
        var found = uiDevice.wait(
            Until.hasObject(By.text(APP_NAME)),
            TIMEOUT
        )
        assertTrue(found)

        // open the Ad Skipper switch
        uiDevice.findObject(By.text(APP_NAME)).click()

        found = uiDevice.wait(
            Until.hasObject(By.clazz(Switch::class.java)),
            TIMEOUT
        )
        assertTrue(found)

        // check if toggle state is different from desired state
        var toggleButton =
            uiDevice.findObjects(By.clazz(Switch::class.java))
                .stream()
                .findFirst()
                .orElse(null)
        val currentState = toggleButton?.isChecked
        if (currentState != null && currentState != enable) {
            toggleButton.clickAndWait(Until.newWindow(), TIMEOUT)
            if (enable) {
                found = uiDevice.findObject(By.text("Allow"))
                    .clickAndWait(Until.newWindow(), TIMEOUT)
                assertTrue(found)
                found = uiDevice.wait(
                    Until.hasObject(By.clazz(Switch::class.java)),
                    TIMEOUT
                )
                assertTrue(found)
                toggleButton = uiDevice.findObject(By.clazz(Switch::class.java)
                    .checked(true))
                assertTrue(toggleButton.isChecked)
            } else {
                var uiObject2 = uiDevice.findObject(By.text("Turn off"))
                if (uiObject2 == null) {
                    uiObject2 = uiDevice.findObject(By.text("Stop"))
                }
                assertNotNull(uiObject2)
                found =
                    uiObject2.clickAndWait(Until.newWindow(), TIMEOUT)
                assertTrue(found)
                found = uiDevice.wait(
                    Until.hasObject(By.clazz(Switch::class.java)),
                    TIMEOUT
                )
                assertTrue(found)
                toggleButton = uiDevice.findObject(By.clazz(Switch::class.java).checked(false))
                assertFalse(toggleButton.isChecked)
            }
        }
        uiDevice.pressBack()
        uiDevice.waitForWindowUpdate(null, TIMEOUT)
        uiDevice.pressBack()
        uiDevice.waitForWindowUpdate(
            MainActivity::class.java.getPackage()?.name,
            TIMEOUT
        )
    }
}