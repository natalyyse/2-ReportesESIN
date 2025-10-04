package com.example.reportes.ui.usuario

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import com.example.reportes.R
import com.example.reportes.data.Remote.FirebaseService
import com.example.reportes.viewmodels.ViewModelUsuario.MainViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import android.app.AlertDialog
import android.provider.Settings


class MainActivity : BaseActivity() {

    private val viewModel: MainViewModel by viewModels()

    // --- Variables de UI y Estado ---
    private lateinit var googleSignInClient: GoogleSignInClient
    private var networkReceiver: BroadcastReceiver? = null
    private val RC_SIGN_IN = 9001
    private var isButtonBlocked = false
    private var alreadyAskedPermission = false
    private var permissionDialog: AlertDialog? = null
    private var wasPermissionRequestedBefore = false

    // --- Vistas ---
    private lateinit var edtPassword: EditText
    private lateinit var edtEmail: EditText
    private lateinit var progressBar: ProgressBar
    private lateinit var progressBackground: View

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_Reportes)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // --- Inicialización y Configuración ---
        initializeViews()
        setupGoogleSignIn()
        setupListeners()
        observeViewModel()
        checkPermissions()
    }

    // --- Inicialización de Vistas ---
    private fun initializeViews() {
        edtPassword = findViewById(R.id.edtPassword)
        edtEmail = findViewById(R.id.edtEmail)
        progressBar = findViewById(R.id.progressBar)
        progressBackground = findViewById(R.id.progressBackground)

        findViewById<ImageView>(R.id.imgPerfil).setImageResource(R.drawable.login)
        findViewById<ImageView>(R.id.imgGoogleLogo).setImageResource(R.drawable.google)
        findViewById<ImageView>(R.id.imgTogglePassword).setImageResource(R.drawable.ic_password_hide)
    }

    // --- Configuración de Listeners ---
    private fun setupListeners() {
        // Mostrar/Ocultar contraseña
        val imgTogglePassword = findViewById<ImageView>(R.id.imgTogglePassword)
        var passwordVisible = false
        imgTogglePassword.setOnClickListener {
            passwordVisible = !passwordVisible
            edtPassword.inputType = if (passwordVisible)
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            else
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD

            imgTogglePassword.setImageResource(
                if (passwordVisible) R.drawable.ic_password_show else R.drawable.ic_password_hide
            )
            edtPassword.setSelection(edtPassword.text.length)
        }

        // Botón Iniciar Sesión (Email/Password)
        findViewById<Button>(R.id.btnIniciar).setOnClickListener {
            handleEmailSignIn()
        }

        // Botón Google Sign-In
        findViewById<LinearLayout>(R.id.btnGoogle).setOnClickListener {
            handleGoogleSignIn()
        }

        // Link para registro
        findViewById<TextView>(R.id.txtLinkRegistro).setOnClickListener {
            if (isButtonBlocked) return@setOnClickListener
            startActivity(Intent(this, RegistrarActivity::class.java))
        }
    }

    // --- Configuración de Google Sign-In ---
    private fun setupGoogleSignIn() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)
    }

    // --- Observador del ViewModel ---
    private fun observeViewModel() {
        viewModel.authResult.observe(this, Observer { result ->
            when (result) {
                is MainViewModel.AuthResult.Loading -> showLoading(true)
                is MainViewModel.AuthResult.Success -> {
                    showLoading(false)
                    goToPrincipal()
                }
                is MainViewModel.AuthResult.Error -> {
                    showLoading(false)
                    // Usar solo blockButtonWithToast para todos los mensajes de error
                    blockButtonWithToast(result.message)
                }
            }
        })
    }

    // --- Manejadores de Acciones ---
    private fun handleEmailSignIn() {
        if (isButtonBlocked) return

        val email = edtEmail.text.toString().trim()
        val password = edtPassword.text.toString()
        if (email.isEmpty() || password.isEmpty()) {
            blockButtonWithToast("Completa todos los campos")
            return
        }
        if (!email.contains("@")) {
            blockButtonWithToast("Correo inválido")
            return
        }
        viewModel.signInWithEmail(email, password)
    }

    private fun handleGoogleSignIn() {
        if (isButtonBlocked) return
        showLoading(true)
        googleSignInClient.signOut().addOnCompleteListener {
            val signInIntent = googleSignInClient.signInIntent
            startActivityForResult(signInIntent, RC_SIGN_IN)
        }
    }

    // --- Resultado de Google Sign-In ---
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_SIGN_IN) {
            try {
                val task = GoogleSignIn.getSignedInAccountFromIntent(data)
                val account = task.getResult(ApiException::class.java)
                
                // Verificamos que el account y el token no sean nulos
                if (account != null && account.idToken != null) {
                    // La validación de existencia y vinculación con Google se hace en el repositorio
                    viewModel.signInWithGoogle(account)
                } else {
                    showLoading(false)
                    blockButtonWithToast("No se pudo obtener la información de Google.")
                    googleSignInClient.signOut()
                }
            } catch (e: ApiException) {
                showLoading(false)
                blockButtonWithToast("Error al iniciar sesión con Google: ${e.localizedMessage}")
                Log.w("LoginGoogle", "Google sign in failed", e)
            }
        } else {
            showLoading(false)
        }
    }

    // --- Helpers de UI ---
    private fun showLoading(isLoading: Boolean) {
        isButtonBlocked = isLoading // Bloquea botones durante la carga
        progressBackground.visibility = if (isLoading) View.VISIBLE else View.GONE
        progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
    }

    private fun blockButtonWithToast(message: String) {
        isButtonBlocked = true
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        Handler(Looper.getMainLooper()).postDelayed({ isButtonBlocked = false }, 2000)
    }

    // --- Navegación ---
    private fun goToPrincipal() {
        startActivity(Intent(this, PrincipalActivity::class.java))
        finish()
    }

    // --- Ciclo de Vida y Permisos ---
    override fun onResume() {
        super.onResume()
        // Cierra el diálogo si está abierto (por si el usuario regresa de Ajustes)
        permissionDialog?.dismiss()
        permissionDialog = null
        checkPermissions()
    }

    private fun checkPermissions() {
        val permissionGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        val shouldShowRationale = shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)

        if (!permissionGranted) {
            if (!shouldShowRationale && wasPermissionRequestedBefore) {
                // Solo muestra el AlertDialog si ya intentaste pedir el permiso antes
                showPermissionExplanationDialog()
            } else {
                // El sistema aún puede mostrar el diálogo nativo
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private val requestPermissionLauncher: androidx.activity.result.ActivityResultLauncher<String> =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            wasPermissionRequestedBefore = true
            // No hagas nada aquí, el ciclo de vida se encargará de volver a pedir si es necesario
        }

    // Launcher para volver de Ajustes. El reinicio ocurre aquí.
    private val settingsLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        // Al volver de ajustes, reinicia la actividad para empezar desde cero
        finish()
        startActivity(intent)
    }

    private fun showPermissionExplanationDialog() {
        if (isFinishing || isDestroyed) return
        if (permissionDialog?.isShowing == true) return // Evita parpadeo

        permissionDialog = AlertDialog.Builder(this)
            .setTitle("Permiso requerido")
            .setMessage("Activa el permiso de cámara para continuar.")
            .setCancelable(false)
            .setPositiveButton("Ir a permisos") { _, _ ->
                permissionDialog?.dismiss()
                permissionDialog = null
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = android.net.Uri.fromParts("package", packageName, null)
                }
                // Usa el launcher en lugar de startActivity y el Handler
                settingsLauncher.launch(intent)
            }
            .setOnDismissListener {
                permissionDialog = null
            }
            .create()
        permissionDialog?.show()
    }
}