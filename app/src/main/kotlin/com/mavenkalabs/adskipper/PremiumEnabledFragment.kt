package com.mavenkalabs.adskipper

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.mavenkalabs.adskipper.util.ProUtils
import kotlinx.coroutines.launch

class PremiumEnabledFragment : Fragment(R.layout.fragment_premium_enabled) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (savedInstanceState == null) {
            val buttonCancel = view.findViewById<Button>(R.id.button_cancel_subscription)
            buttonCancel?.setOnClickListener { _: View? ->
                viewLifecycleOwner.lifecycleScope.launch {
                    val success = ProUtils.navToMgmtUrl(requireContext())
                    if (success) {
                        parentFragmentManager.setFragmentResult("refreshPremiumStatus", bundleOf())
                    } else {
                        Toast.makeText(requireContext(), getString(R.string.error_cancel_subscription), Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }
}