package com.example.reportes.ui.usuario

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.example.reportes.R

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.splash_screen) // Muestra el layout con el logo centrado
        window.statusBarColor = resources.getColor(R.color.colorAccent, theme)
        window.decorView.systemUiVisibility =
            (android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION)
        // Espera 6 segundos y abre la pantalla principal
        Handler(Looper.getMainLooper()).postDelayed({
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }, 2000)
    }
}