package com.mavenkalabs.adskipper;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.mavenkalabs.adskipper.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ActivityMainBinding binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
    }
}