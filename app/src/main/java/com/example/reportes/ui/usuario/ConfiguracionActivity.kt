package com.example.reportes.ui.usuario

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.lifecycle.Observer
import com.example.reportes.R
import com.example.reportes.viewmodels.ViewModelUsuario.ConfiguracionViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.GoogleAuthProvider

class ConfiguracionActivity : BaseActivity() {
    private val viewModel: ConfiguracionViewModel by viewModels()

    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var btnGoogleAction: MaterialButton
    private val RC_GOOGLE_LINK = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_configuracion)

        val tvCorreo = findViewById<TextView>(R.id.tvCorreo)

        viewModel.correo.observe(this, Observer { correo ->
            tvCorreo.text = correo
        })
        viewModel.cargarCorreo()

        findViewById<ImageView>(R.id.btnBack).setOnClickListener {
            val intent = Intent(this, PrincipalActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            finish()
        }

        findViewById<LinearLayout>(R.id.btnAcercaDe).setOnClickListener {
            val intent = Intent(this, AcercaDeActivity::class.java)
            startActivity(intent)
        }

        findViewById<LinearLayout>(R.id.btnEstablecerPassword).setOnClickListener {
            startActivity(Intent(this, EstablecerPasswordActivity::class.java))
        }

        // Configuración de GoogleSignInClient
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        btnGoogleAction = findViewById(R.id.btnGoogleAction)
        val user = com.example.reportes.data.Remote.FirebaseService.auth.currentUser

        val viewGoogleDisabled = findViewById<View>(R.id.viewGoogleDisabled)

        // Verificar si ya está vinculado con Google y actualizar el botón
        val isGoogleLinked = user?.providerData?.any { it.providerId == GoogleAuthProvider.PROVIDER_ID } == true

        if (isGoogleLinked) {
            btnGoogleAction.text = "Vinculado con Google"
            btnGoogleAction.setTextColor(Color.WHITE)
            btnGoogleAction.setOnClickListener(null)
            btnGoogleAction.alpha = 1.0f
            btnGoogleAction.setBackgroundColor(getColorFromResource(R.color.colorAccent))
            btnGoogleAction.strokeWidth = 0
            viewGoogleDisabled.visibility = View.VISIBLE // Mostrar capa semitransparente
        } else {
            btnGoogleAction.text = "Vincular con Google"
            btnGoogleAction.setTextColor(getColorFromResource(R.color.black))
            btnGoogleAction.strokeWidth = 2
            btnGoogleAction.strokeColor = getColorStateListFromColor(getColorFromResource(R.color.colorAccent))
            btnGoogleAction.backgroundTintList = getColorStateListFromColor(Color.TRANSPARENT)
            btnGoogleAction.alpha = 1.0f
            viewGoogleDisabled.visibility = View.GONE // Ocultar capa
            btnGoogleAction.setOnClickListener {
                googleSignInClient.signOut().addOnCompleteListener {
                    val signInIntent = googleSignInClient.signInIntent
                    startActivityForResult(signInIntent, RC_GOOGLE_LINK)
                }
            }
        }
    }

    // Función auxiliar para obtener colores desde recursos de manera compatible
    private fun getColorFromResource(colorResId: Int): Int {
        return resources.getColor(colorResId, theme)
    }
    
    // Función para convertir un color en ColorStateList
    private fun getColorStateListFromColor(color: Int) = android.content.res.ColorStateList.valueOf(color)

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_GOOGLE_LINK) {
            val viewGoogleDisabled = findViewById<View>(R.id.viewGoogleDisabled)
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)
                val googleEmail = account?.email
                val user = com.example.reportes.data.Remote.FirebaseService.auth.currentUser
                val currentEmail = user?.email

                if (googleEmail != null && googleEmail == currentEmail) {
                    val credential = GoogleAuthProvider.getCredential(account.idToken, null)
                    user.linkWithCredential(credential)
                        .addOnCompleteListener { linkTask ->
                            if (linkTask.isSuccessful) {
                                Toast.makeText(this, "Cuenta de Google vinculada correctamente", Toast.LENGTH_SHORT).show()
                                btnGoogleAction.text = "Vinculado con Google"
                                btnGoogleAction.setTextColor(Color.WHITE)
                                btnGoogleAction.setOnClickListener(null)
                                btnGoogleAction.alpha = 1.0f
                                btnGoogleAction.setBackgroundColor(getColorFromResource(R.color.colorAccent))
                                btnGoogleAction.strokeWidth = 0
                                viewGoogleDisabled.visibility = View.VISIBLE // Mostrar capa semitransparente
                            } else {
                                Toast.makeText(this, "Error al vincular: ${linkTask.exception?.localizedMessage}", Toast.LENGTH_LONG).show()
                            }
                        }
                } else {
                    Toast.makeText(this, "Debes seleccionar el mismo correo con el que iniciaste sesión", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this, "Error al seleccionar cuenta de Google", Toast.LENGTH_SHORT).show()
            }
        }
    }
}