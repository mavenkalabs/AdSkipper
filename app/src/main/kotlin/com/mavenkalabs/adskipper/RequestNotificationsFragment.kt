package com.mavenkalabs.adskipper

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment

class RequestNotificationsFragment : Fragment(R.layout.fragment_request_notifications) {
    private val requestNotificationLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        val result = Bundle()
        result.putBoolean("result", isGranted)
        parentFragmentManager.setFragmentResult("refreshNotificationStatus", result)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val btnEnableNotifications = view.findViewById<Button>(R.id.button_enable_notifications)
        btnEnableNotifications?.setOnClickListener { _: View? ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
            ) {
                requestNotificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}