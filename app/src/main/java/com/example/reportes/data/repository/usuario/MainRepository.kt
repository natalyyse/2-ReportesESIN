package com.example.reportes.data.repository.usuario

import android.content.Context
import com.example.reportes.data.Remote.FirebaseService
import com.example.reportes.data.model.User
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestoreException

class MainRepository(private val context: Context) {

    // --- Servicios de Firebase ---
    private val firebaseAuth = FirebaseService.auth
    private val db = FirebaseService.db

    // --- Interfaz de Callback ---
    interface AuthCallback {
        fun onSuccess(user: User)
        fun onError(message: String)
    }

    // --- Métodos de Autenticación ---

    /** Inicia sesión con Email y Password */
    fun signInWithEmail(email: String, password: String, callback: AuthCallback) {
        firebaseAuth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful && firebaseAuth.currentUser != null) {
                    handleSuccessfulAuth(firebaseAuth.currentUser!!, callback)
                } else {
                    val message = when (task.exception) {
                        is FirebaseAuthInvalidUserException,
                        is FirebaseAuthInvalidCredentialsException -> "Credenciales inválidas."
                        else -> "Error al iniciar sesión: ${task.exception?.localizedMessage}"
                    }
                    callback.onError(message)
                }
            }
    }

    /** Inicia sesión con una cuenta de Google */
    fun signInWithGoogle(account: GoogleSignInAccount, callback: AuthCallback) {
        // Primero verificamos si el email existe
        val email = account.email
        if (email == null) {
            callback.onError("No se pudo obtener el email de la cuenta de Google.")
            return
        }

        // Intentamos autenticar directamente con Google
        val credential = GoogleAuthProvider.getCredential(account.idToken, null)
        firebaseAuth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful && firebaseAuth.currentUser != null) {
                    // La autenticación fue exitosa, ahora verificamos si existe en Firestore
                    checkUserExistsInFirestore(firebaseAuth.currentUser!!, callback)
                } else {
                    // Error al autenticar
                    val errorMsg = task.exception?.localizedMessage ?: "Error desconocido"
                    callback.onError("Error al iniciar sesión con Google: $errorMsg")
                    // Si hubo un intento de autenticación, asegurarse de cerrar sesión
                    firebaseAuth.signOut()
                }
            }
    }

    // Método para verificar si el usuario existe en Firestore después de la autenticación
    private fun checkUserExistsInFirestore(firebaseUser: FirebaseUser, callback: AuthCallback) {
        val userEmail = firebaseUser.email
        if (userEmail == null) {
            callback.onError("No se pudo obtener el email del usuario.")
            firebaseAuth.signOut()
            return
        }

        val userDocRef = db.collection("usuarios").document(firebaseUser.uid)
        userDocRef.get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    // El usuario ya existe, usamos su rol guardado
                    val rol = document.getString("rol") ?: "usuario"
                    val userProfile = User(firebaseUser.uid, userEmail, rol)
                    saveUserRole(rol)
                    callback.onSuccess(userProfile)
                } else {
                    // Usuario no existe en Firestore aunque se autenticó con Google
                    // Esto sucede cuando un usuario se autenticó con Google pero no tiene documento en Firestore
                    callback.onError("No existe una cuenta completa con este correo. Por favor regístrese primero.")
                    firebaseAuth.signOut()
                }
            }
            .addOnFailureListener { e ->
                // Si el error es de permisos, probablemente el usuario no tiene acceso según las reglas
                val errorMsg = when (e) {
                    is FirebaseFirestoreException -> {
                        if (e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                            "No tienes permisos para acceder a esta cuenta."
                        } else {
                            "Error al acceder a Firestore: ${e.localizedMessage}"
                        }
                    }
                    else -> "Error al acceder a Firestore: ${e.localizedMessage}"
                }
                callback.onError(errorMsg)
                firebaseAuth.signOut()
            }
    }

    // --- Lógica de Manejo de Perfil de Usuario ---
    private fun handleSuccessfulAuth(firebaseUser: FirebaseUser, callback: AuthCallback) {
        val userEmail = firebaseUser.email
        if (userEmail == null) {
            callback.onError("No se pudo obtener el email del usuario.")
            return
        }

        val userDocRef = db.collection("usuarios").document(firebaseUser.uid)
        userDocRef.get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    // El usuario ya existe, usamos su rol guardado
                    val rol = document.getString("rol") ?: "usuario"
                    val userProfile = User(firebaseUser.uid, userEmail, rol)
                    saveUserRole(rol)
                    callback.onSuccess(userProfile)
                } else {
                    // Para mantener consistencia con nuestra nueva lógica
                    callback.onError("Usuario no registrado en el sistema.")
                    firebaseAuth.signOut() // Cerramos sesión para evitar inconsistencias
                }
            }
            .addOnFailureListener { e ->
                val errorMsg = when (e) {
                    is FirebaseFirestoreException -> {
                        if (e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                            "No tienes permisos para acceder a esta cuenta."
                        } else {
                            "Error al acceder a Firestore: ${e.localizedMessage}"
                        }
                    }
                    else -> "Error al acceder a Firestore: ${e.localizedMessage}"
                }
                callback.onError(errorMsg)
                firebaseAuth.signOut()
            }
    }

    /** Guarda el rol del usuario en SharedPreferences */
    private fun saveUserRole(rol: String) {
        val prefs = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("rol", rol).apply()
    }
}