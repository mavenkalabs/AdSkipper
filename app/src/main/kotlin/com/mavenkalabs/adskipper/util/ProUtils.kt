package com.mavenkalabs.adskipper.util

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import androidx.core.net.toUri
import com.revenuecat.purchases.PurchaseParams
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesException
import com.revenuecat.purchases.awaitOfferings
import com.revenuecat.purchases.awaitPurchase
import com.revenuecat.purchases.models.PurchaseState
import com.revenuecat.purchases.models.StoreProduct

class ProUtils private constructor() {
    companion object {
        @JvmStatic
        suspend fun getProducts(): List<StoreProduct> {
            val offerings = Purchases.sharedInstance.awaitOfferings()
            val packages = offerings.current?.availablePackages

            return packages?.map { it.product }?.toList() ?: emptyList()
        }

        suspend fun purchaseProduct(activity: Activity, product: StoreProduct): Boolean {
            try {
                val params = PurchaseParams.Builder(activity, storeProduct = product)
                    .build()
                val purchaseResult = Purchases.sharedInstance.awaitPurchase(params)

                return purchaseResult.storeTransaction.purchaseState == PurchaseState.PURCHASED
            } catch (_: PurchasesException) {
                return false
            }
        }

        fun launchReviewFlow(activity: Activity) {
            val packageName = activity.packageName

            val marketIntent = Intent(
                Intent.ACTION_VIEW,
                "market://details?id=$packageName".toUri()
            ).apply {
                // Prevents the Play Store from being stacked on top of your app
                addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY or
                        Intent.FLAG_ACTIVITY_NEW_DOCUMENT or
                        Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
            }

            try {
                activity.startActivity(marketIntent)
            } catch (_: ActivityNotFoundException) {
                val webIntent = Intent(
                    Intent.ACTION_VIEW,
                    "https://play.google.com/store/apps/details?id=$packageName".toUri()
                )
                activity.startActivity(webIntent)
            }
        }
    }
}
