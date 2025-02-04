package com.mavenkalabs.adskipper;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.mavenkalabs.adskipper.databinding.FragmentServiceEnabledBinding;
import com.mavenkalabs.adskipper.service.AdSkipperService;

import java.util.Arrays;

public class ServiceEnabledFragment extends Fragment {

    public static final String MUTE_ADS_PREF = "mute_ads";
    private FragmentServiceEnabledBinding binding;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        binding = FragmentServiceEnabledBinding.inflate(inflater, container, false);
        return binding.getRoot();

    }

    @Override
    public void onResume() {
        super.onResume();

        if (!isA11yServiceEnabled()) {
            NavController navController = NavHostFragment.findNavController(this);
            navController.popBackStack();
            navController.navigate(R.id.ServiceDisabledFragment);
        }
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.buttonGotoA11ySettings.setOnClickListener((v) -> {
            Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
            startActivity(intent);
        });

        final SharedPreferences preferences = requireContext().getSharedPreferences(
                requireContext().getPackageName() + "_preferences",
                Context.MODE_PRIVATE);
        if (!preferences.contains(MUTE_ADS_PREF)) {
            preferences.edit().putBoolean(MUTE_ADS_PREF, true).apply();
        }

        binding.checkboxMuteAds.setChecked(
                preferences.getBoolean(MUTE_ADS_PREF, false));

        binding.checkboxMuteAds.setOnCheckedChangeListener((v, isChecked) -> {
            preferences.edit().putBoolean(MUTE_ADS_PREF, isChecked).apply();
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private boolean isA11yServiceEnabled() {
        final String service = requireContext().getPackageName() + "/" + AdSkipperService.class.getCanonicalName();
        try {
            int accessibilityEnabled = Settings.Secure.getInt(requireContext().getApplicationContext().getContentResolver(),
                    Settings.Secure.ACCESSIBILITY_ENABLED);
            if (accessibilityEnabled == 1) {
                String settingValue = Settings.Secure.getString(requireContext().getApplicationContext().getContentResolver(),
                        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
                if (settingValue != null) {
                    return Arrays.stream(settingValue.split(":")).anyMatch(s-> s.equalsIgnoreCase(service));
                }
            }

            return false;
        } catch (Settings.SettingNotFoundException e) {
            return false;
        }
    }
}