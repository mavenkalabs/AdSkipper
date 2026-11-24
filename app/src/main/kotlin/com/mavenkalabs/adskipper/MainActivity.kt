package com.mavenkalabs.adskipper

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.mavenkalabs.adskipper.databinding.ActivityMainBinding
import com.mavenkalabs.adskipper.model.MainViewModel

class MainActivity : AppCompatActivity() {
    private val mainViewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.getRoot())

        handleRequestNotifications(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleRequestNotifications(intent)
    }

    private fun handleRequestNotifications(intent: Intent?) {
        if (intent?.getBooleanExtra("EXTRA_REQUEST_NOTIFICATIONS", false) == true) {
            intent.removeExtra("EXTRA_REQUEST_NOTIFICATIONS")
            mainViewModel.notifyRequestNotifications()
        }
    }
}