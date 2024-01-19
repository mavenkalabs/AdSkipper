package com.mavenkalabs.adskipper;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityManager;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.mavenkalabs.adskipper.databinding.FragmentFirstBinding;
import com.mavenkalabs.adskipper.service.AdSkipperService;

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
        AccessibilityManager a11yManager = (AccessibilityManager)requireContext()
                .getSystemService(Context.ACCESSIBILITY_SERVICE);
        if (a11yManager == null) {
            return false;
        } else {
            return a11yManager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_GENERIC)
                    .stream().anyMatch(info -> {
                        ServiceInfo serviceInfo = info.getResolveInfo().serviceInfo;
                        return serviceInfo.packageName.equals(requireContext().getPackageName()) &&
                                serviceInfo.name.equals(AdSkipperService.class.getName());
                    });
        }
    }
}