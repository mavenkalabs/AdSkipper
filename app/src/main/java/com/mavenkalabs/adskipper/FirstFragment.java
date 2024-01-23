package com.mavenkalabs.adskipper;

import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.mavenkalabs.adskipper.databinding.FragmentFirstBinding;
import com.mavenkalabs.adskipper.service.AdSkipperService;

import java.util.Arrays;

public class FirstFragment extends Fragment {

    private FragmentFirstBinding binding;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {

        binding = FragmentFirstBinding.inflate(inflater, container, false);
        return binding.getRoot();

    }

    @Override
    public void onResume() {
        super.onResume();

        TextView textView = binding.getRoot().findViewById(R.id.textview_first);
        if (textView != null) {
            if (isA11yServiceEnabled()) {
                textView.setText(R.string.a11y_service_enabled_message);
            } else {
                textView.setText(R.string.a11y_service_disabled_message);
            }
        }
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.buttonFirst.setOnClickListener((v) -> {
            Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
            startActivity(intent);
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