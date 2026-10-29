package com.mavenkalabs.adskipper

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.mavenkalabs.adskipper.util.ProUtils
import com.revenuecat.purchases.models.StoreProduct
import kotlinx.coroutines.launch

class PremiumDisabledFragment : Fragment(R.layout.fragment_premium_disabled) {
    private var selectedProduct : StoreProduct? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (savedInstanceState == null) {
            val spinnerPurchaseOptions =
                view.findViewById<Spinner>(R.id.spinner_premium_subscription)
            viewLifecycleOwner.lifecycleScope.launch {
                val products = ProUtils.getProducts()
                val adapter = ArrayAdapter(
                    requireContext(),
                    android.R.layout.simple_spinner_item,
                    products.map {
                        val cleanTitle = it.title.split("(").first().trim()

                        val formattedPrice = it.price.formatted

                        "$cleanTitle — $formattedPrice"
                    }.toList()
                )

                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spinnerPurchaseOptions.adapter = adapter

                spinnerPurchaseOptions.onItemSelectedListener =
                    object : AdapterView.OnItemSelectedListener {
                        override fun onItemSelected(
                            parent: AdapterView<*>?,
                            view: View?,
                            position: Int,
                            id: Long
                        ) {
                            selectedProduct = products[position]
                        }

                        override fun onNothingSelected(parent: AdapterView<*>?) {}
                    }
            }

            val buttonRestorePurchase = view.findViewById<Button>(R.id.button_restore_purchase)
            buttonRestorePurchase?.setOnClickListener { _: View? ->
                buttonRestorePurchase.isEnabled = false
                viewLifecycleOwner.lifecycleScope.launch {
                    try {
                        val success = ProUtils.restorePurchase()
                        if (!success) {
                            Toast.makeText(
                                requireContext(),
                                "Failed to restore subscription",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } finally {
                        buttonRestorePurchase.isEnabled = true
                    }
                }
            }

            val buttonBuy = view.findViewById<Button>(R.id.button_buy)
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
                                        "Failed to purchase subscription",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            } finally {
                                buttonBuy.isEnabled = true
                            }
                        }
                    }
                }
            }
        }
    }
}