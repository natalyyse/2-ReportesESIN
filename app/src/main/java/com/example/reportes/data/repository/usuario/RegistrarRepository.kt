package com.example.reportes.data.repository.usuario

import com.example.reportes.data.model.User
import com.example.reportes.data.Remote.FirebaseService
import com.google.firebase.auth.FirebaseAuthUserCollisionException

// Repositorio que maneja las operaciones de datos para el registro de usuarios.
class RegistrarRepository {
    private val firebaseAuth = FirebaseService.auth
    private val db = FirebaseService.db

    // Define los posibles resultados de la operación de registro.
    sealed class Result {
        object Success : Result() // Éxito
        data class Error(val message: String) : Result() // Error con mensaje
    }

    /**
     * Registra un nuevo usuario en Firebase Authentication y guarda su perfil en Firestore.
     * @param email Correo del usuario.
     * @param password Contraseña del usuario.
     * @param callback Función para devolver el resultado de la operación (éxito o error).
     */
    fun registrarUsuario(
        email: String,
        password: String,
        callback: (Result) -> Unit
    ) {
        // Crea el usuario con email y contraseña en Firebase Auth.
        firebaseAuth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = firebaseAuth.currentUser
                    if (user != null && user.email != null) {
                        // Verifica si el email del usuario existe como "responsable" en algún reporte.
                        db.collection("reportes")
                            .whereEqualTo("responsable", email)
                            .get()
                            .addOnSuccessListener { querySnapshot ->
                                // Asigna el rol "responsable" si se encuentra, si no, "usuario".
                                val rol = if (!querySnapshot.isEmpty) "responsable" else "usuario"
                                val userProfile = User(user.uid, user.email!!, rol)
                                // Guarda el perfil del usuario en la colección "usuarios" de Firestore.
                                db.collection("usuarios").document(user.uid)
                                    .set(userProfile)
                                    .addOnSuccessListener {
                                        callback(Result.Success)
                                    }
                                    .addOnFailureListener { e ->
                                        callback(Result.Error("Error al guardar usuario: ${e.localizedMessage}"))
                                    }
                            }
                            .addOnFailureListener { e ->
                                callback(Result.Error("Error al verificar reportes: ${e.localizedMessage}"))
                            }
                    } else {
                        callback(Result.Error("No se pudo obtener la información del usuario."))
                    }
                } else {
                    // Manejo de errores durante la creación del usuario.
                    val exception = task.exception
                    when {
                        exception is FirebaseAuthUserCollisionException -> {
                            callback(Result.Error("El correo ya está registrado."))
                        }
                        else -> {
                            callback(Result.Error("Error al registrar: ${exception?.localizedMessage}"))
                        }
                    }
                }
            }
    }
}