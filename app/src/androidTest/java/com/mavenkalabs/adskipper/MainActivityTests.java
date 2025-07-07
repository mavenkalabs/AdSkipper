package com.mavenkalabs.adskipper;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.provider.Settings;
import android.widget.Switch;
import android.widget.VideoView;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.uiautomator.By;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject2;
import androidx.test.uiautomator.Until;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Objects;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;
import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class MainActivityTests {
    private UiDevice uiDevice;

    private static final long TIMEOUT = 2000;
    private static final String APP_NAME = "Ad Skipper";

    @Before
    public void setupBefore() {
        uiDevice = UiDevice.getInstance(getInstrumentation());
    }

    @Test
    public void verifyUiAfterDisagree() {
        Intent activityIntent = new Intent(getApplicationContext(), MainActivity.class);
        activityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        getApplicationContext().startActivity(activityIntent);

        boolean found = uiDevice.wait(Until.hasObject(By.pkg(Objects.requireNonNull(MainActivity.class.getPackage()).getName())
                .depth(0)), TIMEOUT);
        assertTrue(found);

        // verify UI on first launch and then go to Accessibility Settings
        found = uiDevice.hasObject(By.text(
                getApplicationContext().getString(R.string.a11y_service_disabled_message)));
        assertTrue(found);

        found = uiDevice.findObject(
                By.text(getApplicationContext().getString(R.string.disagree).toUpperCase())
        ).clickAndWait(Until.newWindow(), TIMEOUT);
        assertTrue(found);

        assertNull(uiDevice.findObject(
                By.pkg(Objects.requireNonNull(MainActivity.class.getPackage()).getName())));
    }

    @Test
    public void verifyUiAfterEnabling() {
        Intent activityIntent = new Intent(getApplicationContext(), MainActivity.class);
        activityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        getApplicationContext().startActivity(activityIntent);

        boolean found = uiDevice.wait(Until.hasObject(By.pkg(Objects.requireNonNull(MainActivity.class.getPackage()).getName())
                .depth(0)), TIMEOUT);
        assertTrue(found);

        // verify UI on first launch and then go to Accessibility Settings
        found = uiDevice.hasObject(By.text(
                getApplicationContext().getString(R.string.a11y_service_disabled_message)));
        assertTrue(found);

        found = uiDevice.findObject(
                By.text(getApplicationContext().getString(R.string.agree).toUpperCase())
        ).clickAndWait(Until.newWindow(), TIMEOUT);
        assertTrue(found);

        // toggle the accessibility feature on
        toggleA11ySetting(true);

        // expect the service to be enabled now
        found = uiDevice.wait(Until.hasObject(By.text(getApplicationContext().getString(R.string.a11y_service_enabled_message))), 10000);
        assertTrue(found);

        found = uiDevice.wait(Until.hasObject(By.text(getApplicationContext().getString(R.string.mute_ads)).checked(true)), 1000);
        assertTrue(found);

        SharedPreferences prefs = getApplicationContext().getSharedPreferences(
                getApplicationContext().getPackageName() + "_preferences",
                Context.MODE_PRIVATE);
        assertTrue(prefs.getBoolean(ServiceEnabledFragment.MUTE_ADS_PREF, false));

        uiDevice.findObject(By.text(getApplicationContext().getString(R.string.mute_ads))).click();
        found = uiDevice.wait(Until.hasObject(By.text(getApplicationContext().getString(R.string.mute_ads)).checked(false)), 1000);
        assertTrue(found);
        assertFalse(prefs.getBoolean(ServiceEnabledFragment.MUTE_ADS_PREF, false));

        uiDevice.findObject(By.text(getApplicationContext().getString(R.string.mute_ads))).click();
        found = uiDevice.wait(Until.hasObject(By.text(getApplicationContext().getString(R.string.mute_ads)).checked(true)), 1000);
        assertTrue(found);
        assertTrue(prefs.getBoolean(ServiceEnabledFragment.MUTE_ADS_PREF, false));
    }
    @Test
    public void verifyUiTutorial() {
        Intent activityIntent = new Intent(getApplicationContext(), MainActivity.class);
        activityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        getApplicationContext().startActivity(activityIntent);

        boolean found = uiDevice.wait(Until.hasObject(By.pkg(Objects.requireNonNull(MainActivity.class.getPackage()).getName())
                .depth(0)), TIMEOUT);
        assertTrue(found);

        found = uiDevice.hasObject(By.text(
                getApplicationContext().getString(R.string.tutorial)));
        assertTrue(found);

        found = uiDevice.findObject(
                By.text(getApplicationContext().getString(R.string.tutorial))
        ).clickAndWait(Until.newWindow(), TIMEOUT);
        assertTrue(found);

        found = uiDevice.hasObject(By.clazz(VideoView.class));
        assertTrue(found);

        found = uiDevice.wait(Until.gone(By.clazz(VideoView.class)), 20000);
        assertTrue(found);
    }

    @Test
    public void verifyUiAfterDisabling() {
        launchA11ySettings();

        toggleA11ySetting(true);

        Intent activityIntent = new Intent(getApplicationContext(), MainActivity.class);
        activityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        getApplicationContext().startActivity(activityIntent);

        boolean found = uiDevice.wait(Until.hasObject(By.pkg(Objects.requireNonNull(MainActivity.class.getPackage()).getName())
                .depth(0)), TIMEOUT);
        assertTrue(found);

        // verify UI on first launch and then go to Accessibility Settings
        found = uiDevice.hasObject(By.text(
                getApplicationContext().getString(R.string.a11y_service_enabled_message)));
        assertTrue(found);

        found = uiDevice.findObject(
                By.text(getApplicationContext().getString(R.string.go_to_a11y_settings).toUpperCase())
        ).clickAndWait(Until.newWindow(), TIMEOUT);
        assertTrue(found);

        // toggle the accessibility feature off
        toggleA11ySetting(false);

        // expect the service to be enabled now
        found = uiDevice.wait(Until.hasObject(By.text(getApplicationContext().getString(R.string.a11y_service_disabled_message))), 10000);
        assertTrue(found);
    }

    private void launchA11ySettings() {
        Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK);
        getInstrumentation().getContext().startActivity(intent);

        uiDevice.waitForWindowUpdate(null, TIMEOUT);
    }

    private void toggleA11ySetting(boolean enable) {
        // wait for it to show up
        boolean found = uiDevice.wait(Until.hasObject(By.text(APP_NAME)), TIMEOUT);
        assertTrue(found);

        // open the Ad Skipper switch
        uiDevice.findObject(By.text(APP_NAME)).click();

        found = uiDevice.wait(Until.hasObject(By.clazz(Switch.class)), TIMEOUT);
        assertTrue(found);

        // check if toggle state is different from desired state
        UiObject2 toggleButton = uiDevice.findObjects(By.clazz(Switch.class)).stream().findFirst().orElse(null);
        boolean currentState = Objects.requireNonNull(toggleButton).isChecked();
        if (currentState != enable) {
            toggleButton.clickAndWait(Until.newWindow(), TIMEOUT);
            if (enable) {
                found = uiDevice.findObject(By.text("Allow")).clickAndWait(Until.newWindow(), TIMEOUT);
                assertTrue(found);
                found = uiDevice.wait(Until.hasObject(By.clazz(Switch.class)), TIMEOUT);
                assertTrue(found);
                toggleButton = uiDevice.findObject(By.clazz(Switch.class).checked(true));
                assertTrue(Objects.requireNonNull(toggleButton).isChecked());
            } else {
                found = uiDevice.findObject(By.text("Stop")).clickAndWait(Until.newWindow(), TIMEOUT);
                assertTrue(found);
                found = uiDevice.wait(Until.hasObject(By.clazz(Switch.class)), TIMEOUT);
                assertTrue(found);
                toggleButton = uiDevice.findObject(By.clazz(Switch.class).checked(false));
                assertFalse(Objects.requireNonNull(toggleButton).isChecked());
            }
        }
        uiDevice.pressBack();
        uiDevice.waitForWindowUpdate(null, TIMEOUT);
        uiDevice.pressBack();
        uiDevice.waitForWindowUpdate(Objects.requireNonNull(MainActivity.class.getPackage()).getName(), TIMEOUT);
    }
}