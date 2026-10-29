package com.mavenkalabs.adskipper.util

import android.app.Activity
import android.content.Context
import android.content.Intent
import com.revenuecat.purchases.PurchaseParams
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesException
import com.revenuecat.purchases.awaitCustomerInfo
import com.revenuecat.purchases.awaitOfferings
import com.revenuecat.purchases.awaitPurchase
import com.revenuecat.purchases.awaitRestore
import com.revenuecat.purchases.models.StoreProduct

class ProUtils private constructor() {
    companion object {
        private const val PRO_ENTITLEMENT = "adskipper_pro"

        private val TAG = ProUtils::class.java.name

        @JvmStatic
        suspend fun isPro(): Boolean {
            val customerInfo = Purchases.sharedInstance.awaitCustomerInfo()
            return customerInfo.entitlements.active[PRO_ENTITLEMENT] != null
        }

        @JvmStatic
        suspend fun getProducts(): List<StoreProduct> {
            val offerings = Purchases.sharedInstance.awaitOfferings()
            val packages = offerings.current?.availablePackages

            return packages?.map { it.product }?.toList() ?: emptyList()
        }

        @JvmStatic
        suspend fun navToMgmtUrl(context: Context) : Boolean {
            val customerInfo = Purchases.sharedInstance.awaitCustomerInfo()
            if (customerInfo.managementURL != null) {
                try {
                    val intent = Intent(Intent.ACTION_VIEW, customerInfo.managementURL).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                } catch (e: Exception) {
                    AppLog.e(TAG, e.message?:e.toString(), e)
                }
            }

            return false
        }

        suspend fun purchaseProduct(activity: Activity, product: StoreProduct): Boolean {
            try {
                val purchaseResult = Purchases.sharedInstance.awaitPurchase(
                    PurchaseParams.Builder(activity, product).build()
                )

                return purchaseResult.customerInfo.entitlements[PRO_ENTITLEMENT]?.isActive?:false
            } catch (_: PurchasesException) {
                return false
            }
        }

        suspend fun restorePurchase(): Boolean {
            try {
                val customerInfo = Purchases.sharedInstance.awaitRestore()

                return customerInfo.entitlements[PRO_ENTITLEMENT]?.isActive?:false
            } catch (_: PurchasesException) {
                return false
            }
        }
    }
}
