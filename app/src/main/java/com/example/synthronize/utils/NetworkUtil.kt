package com.example.synthronize.utils
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.view.View
import com.example.synthronize.interfaces.OnNetworkRetryListener
import com.google.android.material.snackbar.Snackbar

class NetworkUtil(private val context: Context) {

    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private lateinit var snackbar: Snackbar

    fun checkNetworkAndShowSnackbar(view: View, retryListener: OnNetworkRetryListener) {
        if (!isNetworkAvailable()) {
            // Show Snackbar with retry action
            snackbar = Snackbar.make(view, "No Internet Connection", Snackbar.LENGTH_INDEFINITE)
                .setAction("Retry") {
                    retryListener.retryNetwork()
                    snackbar.dismiss()
                }
            snackbar.show()
        }
    }

    private fun isNetworkAvailable(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val activeNetwork = connectivityManager.activeNetwork ?: return false
            val networkCapabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
            networkCapabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        } else {
            @Suppress("DEPRECATION")
            val networkInfo = connectivityManager.activeNetworkInfo
            networkInfo?.isConnected == true
        }
    }
}
