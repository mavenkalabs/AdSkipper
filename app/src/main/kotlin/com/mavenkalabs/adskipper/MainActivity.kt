package com.mavenkalabs.adskipper

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.mavenkalabs.adskipper.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.getRoot())
    }
}