package com.example.reportes.ui.usuario

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.reportes.R
import com.example.reportes.data.Remote.FirebaseService
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseUser

class EstablecerPasswordActivity : AppCompatActivity() {
    private lateinit var etPassword: EditText
    private lateinit var etConfirmPassword: EditText
    private lateinit var etCurrentPassword: EditText
    private lateinit var btnGuardar: Button
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_establecer_password)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        etPassword = findViewById(R.id.etPassword)
        etConfirmPassword = findViewById(R.id.etConfirmPassword)
        etCurrentPassword = findViewById(R.id.etCurrentPassword) 
        btnGuardar = findViewById(R.id.btnGuardar)
        
        // Verificar si el usuario ya tiene un proveedor de email/contraseña
        val currentUser = FirebaseService.auth.currentUser
        val hasPasswordProvider = currentUser?.providerData?.any { it.providerId == EmailAuthProvider.PROVIDER_ID } ?: false
        
        // Cambiar el título según el caso: agregar o actualizar contraseña
        val tvTitulo = findViewById<TextView>(R.id.tvTitulo)
        if (hasPasswordProvider) {
            tvTitulo.text = "Actualizar contraseña"
        } else {
            tvTitulo.text = "Agregar contraseña"
        }
        
        // Configurar la visibilidad del campo contraseña actual
        etCurrentPassword.visibility = if (hasPasswordProvider) View.VISIBLE else View.GONE
        findViewById<TextView>(R.id.tvCurrentPasswordLabel)?.visibility = 
            if (hasPasswordProvider) View.VISIBLE else View.GONE

        btnGuardar.setOnClickListener {
            val password = etPassword.text.toString()
            val confirmPassword = etConfirmPassword.text.toString()
            val currentPassword = etCurrentPassword.text.toString()

            if (password.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(this, "Completa ambos campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            if (password != confirmPassword) {
                Toast.makeText(this, "Las contraseñas no coinciden", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (hasPasswordProvider) {
                if (currentPassword.isEmpty()) {
                    Toast.makeText(this, "Ingresa tu contraseña actual", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                
                // Verificar que la nueva contraseña sea diferente de la actual
                if (currentPassword == password) {
                    Toast.makeText(this, "La nueva contraseña debe ser diferente a la actual", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
            }

            val email = currentUser?.email
            if (email != null) {
                if (hasPasswordProvider) {
                    // Caso de actualización de contraseña
                    updatePassword(currentUser, currentPassword, password)
                } else {
                    // Caso de vinculación por primera vez
                    linkEmailPassword(email, password)
                }
            } else {
                Toast.makeText(this, "No se encontró usuario autenticado", Toast.LENGTH_SHORT).show()
            }
        }

        // Flecha de regreso
        findViewById<ImageView>(R.id.btnBack).setOnClickListener {
            finish()
        }
    }
    
    private fun updatePassword(user: FirebaseUser, currentPassword: String, newPassword: String) {
        val email = user.email ?: return
        
        // Re-autenticar al usuario
        val credential = EmailAuthProvider.getCredential(email, currentPassword)
        user.reauthenticate(credential)
            .addOnCompleteListener { reAuthTask ->
                if (reAuthTask.isSuccessful) {
                    // Actualizar contraseña
                    user.updatePassword(newPassword)
                        .addOnCompleteListener { updateTask ->
                            if (updateTask.isSuccessful) {
                                Toast.makeText(this, "Contraseña actualizada correctamente", Toast.LENGTH_SHORT).show()
                                finish()
                            } else {
                                val errorMsg = when (updateTask.exception) {
                                    is FirebaseAuthInvalidCredentialsException -> "La contraseña debe tener al menos 6 caracteres"
                                    else -> "Error al actualizar: ${updateTask.exception?.localizedMessage}"
                                }
                                Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show()
                            }
                        }
                } else {
                    Toast.makeText(this, "Contraseña actual incorrecta", Toast.LENGTH_SHORT).show()
                }
            }
    }
    
    private fun linkEmailPassword(email: String, password: String) {
        val task = FirebaseService.linkEmailAndPassword(email, password)
        if (task != null) {
            task.addOnCompleteListener { taskResult ->
                if (taskResult.isSuccessful) {
                    Toast.makeText(this, "Contraseña vinculada correctamente", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    val errorMsg = when (taskResult.exception) {
                        is FirebaseAuthInvalidCredentialsException -> "La contraseña debe tener al menos 6 caracteres"
                        else -> "Error al vincular: ${taskResult.exception?.localizedMessage}"
                    }
                    Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show()
                }
            }
        } else {
            Toast.makeText(this, "No se encontró usuario autenticado", Toast.LENGTH_SHORT).show()
        }
    }
}