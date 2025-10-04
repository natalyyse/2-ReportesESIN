package com.example.reportes.ui.usuario

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.example.reportes.R

class AcercaDeActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_acerca_de)

        // Botón de regreso
        findViewById<ImageView>(R.id.btnBackAcerca).setOnClickListener {
            finish() // Cierra esta pantalla y regresa
        }

        // Botón de "Más información"
        findViewById<Button>(R.id.btnMasInfo).setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://esin.com.pe/"))
            startActivity(intent)
        }
    }
}