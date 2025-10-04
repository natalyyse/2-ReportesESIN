package com.example.reportes.data.repository.usuario

import com.example.reportes.data.Remote.FirebaseService

class ConfiguracionRepository {
    fun obtenerCorreoUsuario(
        onSuccess: (String) -> Unit,
        onFailure: (String) -> Unit
    ) {
        val user = FirebaseService.auth.currentUser
        if (user != null) {
            val db = FirebaseService.db
            db.collection("usuarios").document(user.uid).get()
                .addOnSuccessListener { document ->
                    val correo = document.getString("email") ?: user.email ?: "correo@gmail.com"
                    onSuccess(correo)
                }
                .addOnFailureListener {
                    onFailure(user.email ?: "correo@gmail.com")
                }
        } else {
            onFailure("correo@gmail.com")
        }
    }
}