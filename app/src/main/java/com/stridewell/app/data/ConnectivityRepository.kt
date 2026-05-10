package com.stridewell.app.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Global online/offline source of truth backed by ConnectivityManager.
 * Screens read `isOffline` to decide whether to show the offline banner
 * or disable network-dependent actions (e.g. chat send).
 */
@Singleton
class ConnectivityRepository @Inject constructor(
    @ApplicationContext context: Context
) {

    private val _isOffline = MutableStateFlow(initialOffline(context))
    val isOffline: StateFlow<Boolean> = _isOffline.asStateFlow()

    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            _isOffline.value = false
        }

        override fun onLost(network: Network) {
            _isOffline.value = !hasAnyValidatedNetwork()
        }

        override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) {
            if (caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) {
                _isOffline.value = false
            }
        }
    }

    init {
        connectivityManager.registerDefaultNetworkCallback(callback)
    }

    private fun hasAnyValidatedNetwork(): Boolean {
        val active = connectivityManager.activeNetwork ?: return false
        val caps = connectivityManager.getNetworkCapabilities(active) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    companion object {
        private fun initialOffline(context: Context): Boolean {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val active = cm.activeNetwork ?: return true
            val caps = cm.getNetworkCapabilities(active) ?: return true
            return !caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        }
    }
}
