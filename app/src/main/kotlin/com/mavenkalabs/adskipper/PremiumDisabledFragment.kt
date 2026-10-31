package com.mavenkalabs.adskipper

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.mavenkalabs.adskipper.util.ProUtils
import com.revenuecat.purchases.models.StoreProduct
import kotlinx.coroutines.launch

class PremiumDisabledFragment : Fragment(R.layout.fragment_premium_disabled) {
    private var selectedProduct : StoreProduct? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val buttonBuy = view.findViewById<Button>(R.id.button_buy)
        viewLifecycleOwner.lifecycleScope.launch {
            val products = ProUtils.getProducts()
            selectedProduct = products.firstOrNull()

            if (selectedProduct == null) {
                buttonBuy.visibility = View.GONE
            }
        }

        buttonBuy?.setOnClickListener { _: View? ->
            if (selectedProduct != null) {
                val productForPurchase = selectedProduct
                productForPurchase?.let {
                    buttonBuy.isEnabled = false
                    viewLifecycleOwner.lifecycleScope.launch {
                        try {
                            val purchaseSuccess =
                                ProUtils.purchaseProduct(requireActivity(), productForPurchase)
                            if (!purchaseSuccess) {
                                Toast.makeText(
                                    requireContext(),
                                    getString(R.string.error_purchase_subscription),
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                parentFragmentManager.setFragmentResult("refreshPremiumStatus", bundleOf())
                            }
                        } finally {
                            buttonBuy.isEnabled = true
                        }
                    }
                }
            }
        }

        val buttonReview = view.findViewById<Button>(R.id.button_review)

        buttonReview?.setOnClickListener { _: View? ->
            viewLifecycleOwner.lifecycleScope.launch {
                ProUtils.launchReviewFlow(requireActivity())
            }
        }
    }
}