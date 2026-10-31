package com.mavenkalabs.adskipper

import android.app.Application
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesConfiguration
import com.revenuecat.purchases.LogLevel

const val REVCAT_API_KEY = "test_gYbxHvTUKhmJljLcCpYfPGzvWSR"

class MainApplication: Application() {
    override fun onCreate() {
        super.onCreate()
        Purchases.logLevel = LogLevel.DEBUG
        Purchases.configure(PurchasesConfiguration.Builder(this, REVCAT_API_KEY)
            .build())
    }
}
