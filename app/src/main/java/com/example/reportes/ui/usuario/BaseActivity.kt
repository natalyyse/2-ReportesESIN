package com.example.reportes.ui.usuario

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

abstract class BaseActivity : AppCompatActivity() {

    private var noInternetDialog: NoInternet? = null

    // Aquí mismo declaramos el receiver
    private val networkReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val connectivityManager =
                context?.getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
            val activeNetwork = connectivityManager.activeNetworkInfo
            val isConnected = activeNetwork != null && activeNetwork.isConnected

            if (!isConnected) {
                showNoInternetOverlay()
                Toast.makeText(context, "No tienes conexión a Internet", Toast.LENGTH_SHORT).show()
            } else {
                hideNoInternetOverlay()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val filter = IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
        registerReceiver(networkReceiver, filter)
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(networkReceiver)
    }

    private fun showNoInternetOverlay() {
        if (noInternetDialog?.isVisible != true) {
            noInternetDialog = NoInternet()
            noInternetDialog?.show(supportFragmentManager, "NoInternetDialog")
        }
    }

    private fun hideNoInternetOverlay() {
        noInternetDialog?.dismiss()
    }
}