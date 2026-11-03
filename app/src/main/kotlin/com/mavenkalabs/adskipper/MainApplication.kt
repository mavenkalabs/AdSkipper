package com.mavenkalabs.adskipper

import android.app.Application
import com.mavenkalabs.adskipper.util.ProUtils.Companion.initPurchases


class MainApplication: Application() {
    override fun onCreate() {
        super.onCreate()

        initPurchases(this)
    }
}
