package com.mavenkalabs.adskipper;

import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.DialogFragment;

import com.mavenkalabs.adskipper.databinding.FragmentTutorialBinding;

public class TutorialFragment extends DialogFragment {
    public static final String TAG = TutorialFragment.class.getSimpleName();
    private FragmentTutorialBinding binding;

    @androidx.annotation.Nullable
    @Override
    public View onCreateView(@androidx.annotation.NonNull LayoutInflater inflater, @androidx.annotation.Nullable ViewGroup container, @androidx.annotation.Nullable Bundle savedInstanceState) {
        binding = FragmentTutorialBinding.inflate(inflater, container, false);
        boolean nightMode = (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;

        binding.videoTutorial.setVideoURI(Uri.parse("android.resource://"
                + requireContext().getPackageName()
                + "/"
                + (nightMode ? R.raw.tutorial_dark : R.raw.tutorial_light)));
        binding.videoTutorial.start();
        binding.videoTutorial.setOnCompletionListener((player) -> dismiss());

        return binding.getRoot();
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
