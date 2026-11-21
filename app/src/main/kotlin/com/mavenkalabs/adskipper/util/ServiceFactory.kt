package com.mavenkalabs.adskipper.util

import android.content.Context
import androidx.annotation.VisibleForTesting
import androidx.work.WorkManager

object ServiceFactory {
    private var workManagerForTesting: WorkManager? = null

    @VisibleForTesting
    fun setWorkManager(workManager: WorkManager) {
        workManagerForTesting = workManager
    }

    fun getWorkManager(context: Context) : WorkManager {
        return workManagerForTesting?: WorkManager.getInstance(context)
    }
}