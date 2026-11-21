package com.mavenkalabs.adskipper.model

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class MainViewModel : ViewModel() {
    private val _requestNotifications = MutableStateFlow(false)
    val requestNotifications = _requestNotifications.asStateFlow()

    fun notifyRequestNotifications() {
        _requestNotifications.value = true
    }

    fun consumeRequestNotificationsEvent() {
        _requestNotifications.value = false
    }
}
