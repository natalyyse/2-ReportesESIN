package com.example.reportes.ui.usuario

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.*
import androidx.activity.viewModels
import androidx.lifecycle.Observer
import com.example.reportes.R
import com.example.reportes.data.model.User
import com.example.reportes.viewmodels.ViewModelUsuario.RegistrarViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider

class RegistrarActivity : BaseActivity() {
    private val viewModel: RegistrarViewModel by viewModels()
    private lateinit var googleSignInClient: GoogleSignInClient
    private val RC_GOOGLE_REGISTER = 2001
    private lateinit var progressBar: ProgressBar
    private lateinit var progressBackground: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registrar)

        // Inicialización de vistas
        val imgPerfil = findViewById<ImageView>(R.id.imgPerfil)
        imgPerfil.setImageResource(R.drawable.login)

        val edtEmail = findViewById<EditText>(R.id.edtEmail)
        val edtPassword = findViewById<EditText>(R.id.edtPassword)
        val edtConfirmPassword = findViewById<EditText>(R.id.edtConfirmPassword)
        val btnRegistrar = findViewById<Button>(R.id.btnRegistrar)
        val btnGoogleRegister = findViewById<Button>(R.id.btnGoogleRegister)
        val txtLinkLogin = findViewById<TextView>(R.id.txtLinkLogin)
        progressBar = findViewById(R.id.progressBar)
        progressBackground = findViewById(R.id.progressBackground)
        val imgTogglePassword = findViewById<ImageView>(R.id.imgTogglePassword)
        val imgToggleConfirmPassword = findViewById<ImageView>(R.id.imgToggleConfirmPassword)

        // Configuración de la funcionalidad para mostrar/ocultar contraseña
        setupPasswordToggle(imgTogglePassword, edtPassword)
        setupPasswordToggle(imgToggleConfirmPassword, edtConfirmPassword)

        // Configurar Google Sign-In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        // Botón para registrar con Google
        btnGoogleRegister.setOnClickListener {
            showProgress(true)
            googleSignInClient.signOut().addOnCompleteListener {
                val signInIntent = googleSignInClient.signInIntent
                startActivityForResult(signInIntent, RC_GOOGLE_REGISTER)
            }
        }

        // Navegación a Login
        txtLinkLogin.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
        }

        // Botón de registro normal
        btnRegistrar.setOnClickListener {
            viewModel.registrarUsuario(
                edtEmail.text.toString().trim(),
                edtPassword.text.toString(),
                edtConfirmPassword.text.toString()
            )
        }

        // Observer para estados de UI
        viewModel.uiState.observe(this, Observer { state ->
            when (state) {
                is RegistrarViewModel.UiState.Loading -> showProgress(true)
                is RegistrarViewModel.UiState.Success -> {
                    showProgress(false)
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                }
                is RegistrarViewModel.UiState.Error -> {
                    showProgress(false)
                    Toast.makeText(this, state.message, Toast.LENGTH_SHORT).show()
                }
                is RegistrarViewModel.UiState.Idle -> showProgress(false)
            }
        })
    }

    private fun showProgress(show: Boolean) {
        progressBackground.visibility = if (show) View.VISIBLE else View.GONE
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_GOOGLE_REGISTER) {
            try {
                val task = GoogleSignIn.getSignedInAccountFromIntent(data)
                val account = task.getResult(ApiException::class.java)

                if (account != null) {
                    verificarYRegistrarConGoogle(account)
                } else {
                    showProgress(false)
                    Toast.makeText(this, "No se pudo obtener la cuenta de Google", Toast.LENGTH_SHORT).show()
                }
            } catch (e: ApiException) {
                showProgress(false)
                Toast.makeText(this, "Error al seleccionar cuenta: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun verificarYRegistrarConGoogle(account: GoogleSignInAccount) {
        if (account.email != null) {
            crearCuentaConGoogle(account)
        } else {
            showProgress(false)
            Toast.makeText(this, "No se pudo obtener el correo de la cuenta", Toast.LENGTH_SHORT).show()
        }
    }

    private fun crearCuentaConGoogle(account: GoogleSignInAccount) {
        val credential = GoogleAuthProvider.getCredential(account.idToken, null)

        // Autenticar directamente con credencial de Google
        com.example.reportes.data.Remote.FirebaseService.auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // Verificar si es un usuario nuevo
                    val isNewUser = task.result?.additionalUserInfo?.isNewUser ?: false
                    
                    if (isNewUser) {
                        // Es un usuario nuevo, guardar perfil en Firestore
                        val user = com.example.reportes.data.Remote.FirebaseService.auth.currentUser
                        if (user != null) {
                            guardarPerfilUsuario(user)
                        } else {
                            showProgress(false)
                            Toast.makeText(this, "Error al obtener usuario", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        // Usuario ya existente, cerrar sesión pero mantenerse en la pantalla
                        showProgress(false)
                        Toast.makeText(this, "El usuario ya existe.", Toast.LENGTH_LONG).show()
                        FirebaseAuth.getInstance().signOut()
                        googleSignInClient.signOut().addOnCompleteListener {
                            // No hacer nada aquí para mantenerse en la pantalla actual
                        }
                    }
                } else {
                    showProgress(false)
                    FirebaseAuth.getInstance().signOut()
                    Toast.makeText(this, "Error en la autenticación: ${task.exception?.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun guardarPerfilUsuario(user: FirebaseUser) {
        // Guardar el perfil en Firestore
        val db = com.example.reportes.data.Remote.FirebaseService.db
        val userProfile = User(user.uid, user.email!!, "usuario")

        db.collection("usuarios").document(user.uid)
            .set(userProfile)
            .addOnSuccessListener {
                showProgress(false)
                startActivity(Intent(this, PrincipalActivity::class.java))
                finish()
            }
            .addOnFailureListener { e ->
                showProgress(false)
                FirebaseAuth.getInstance().signOut()
                Toast.makeText(this, "Error al guardar perfil: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun setupPasswordToggle(toggleImageView: ImageView, passwordEditText: EditText) {
        var passwordVisible = false
        toggleImageView.setImageResource(R.drawable.ic_password_hide)
        toggleImageView.setOnClickListener {
            passwordVisible = !passwordVisible
            passwordEditText.inputType = if (passwordVisible)
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            else
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            toggleImageView.setImageResource(
                if (passwordVisible) R.drawable.ic_password_show
                else R.drawable.ic_password_hide
            )
            passwordEditText.setSelection(passwordEditText.text.length)
        }
    }
}