package com.mavenkalabs.adskipper

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.widget.Switch
import android.widget.VideoView
import androidx.preference.PreferenceManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import com.mavenkalabs.adskipper.service.AdSkipperService
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertNotNull
import junit.framework.TestCase.assertNull
import junit.framework.TestCase.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Locale

private const val TIMEOUT : Long = 2000
private const val APP_NAME: String = "Ad Skipper"

@RunWith(AndroidJUnit4::class)
internal class MainActivityTests {
    private lateinit var uiDevice: UiDevice

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
                    .getString(R.string.a11y_service_disabled_message)
            )
        )
        assertTrue(found)

        found = uiDevice.findObject(
            By.text(
                ApplicationProvider.getApplicationContext<Context>().getString(R.string.disagree)
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
                    .getString(R.string.a11y_service_disabled_message)
            )
        )
        assertTrue(found)

        found = uiDevice.findObject(
            By.text(
                ApplicationProvider.getApplicationContext<Context>().getString(R.string.agree)
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
                        .getString(R.string.a11y_service_enabled_message)
                )
            ), 10000
        )
        assertTrue(found)

        val prefs =
            PreferenceManager.getDefaultSharedPreferences(
                ApplicationProvider.getApplicationContext())
        assertTrue(prefs.getBoolean(AdSkipperService.MUTE_ADS_PREF, false))

        launchA11ySettings()
        toggleMuteAdsSetting(false)
        assertFalse(prefs.getBoolean(AdSkipperService.MUTE_ADS_PREF, true))

        toggleMuteAdsSetting(true)
        assertTrue(prefs.getBoolean(AdSkipperService.MUTE_ADS_PREF, false))
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
                ApplicationProvider.getApplicationContext<Context>().getString(R.string.tutorial)
            )
        )
        assertTrue(found)

        found = uiDevice.findObject(
            By.text(
                ApplicationProvider.getApplicationContext<Context>().getString(R.string.tutorial)
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
                    .getString(R.string.a11y_service_enabled_message)
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
                        .getString(R.string.a11y_service_disabled_message)
                )
            ), 10000
        )
        assertTrue(found)
    }

    private fun launchA11ySettings() {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        InstrumentationRegistry.getInstrumentation().context.startActivity(intent)

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
                        .getString(R.string.mute_ads)
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