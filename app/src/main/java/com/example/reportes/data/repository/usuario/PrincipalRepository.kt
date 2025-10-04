package com.example.reportes.data.repository.usuario

import com.example.reportes.data.Remote.FirebaseService
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

/**
 * Repositorio para manejar los datos de la pantalla principal,
 * como el rol del usuario y sus reportes asignados.
 */
class PrincipalRepository {
    // Instancias para los listeners de Firestore, permitiendo su posterior eliminación.
    private var rolListener: ListenerRegistration? = null
    private var reportesListener: ListenerRegistration? = null
    private val db = FirebaseService.db
    private val auth = FirebaseService.auth

    /**
     * Escucha en tiempo real los cambios en el rol del usuario actual.
     * @param onRolChange Callback que se ejecuta con el nuevo rol.
     */
    fun escucharRol(onRolChange: (String) -> Unit) {
        val userEmail = auth.currentUser?.email ?: return

        // Remueve el listener anterior para evitar duplicados.
        rolListener?.remove()
        // Escucha cambios en el documento del usuario para obtener su rol.
        rolListener = db.collection("usuarios")
            .whereEqualTo("email", userEmail)
            .addSnapshotListener { snapshots, _ ->
                if (snapshots != null && !snapshots.isEmpty) {
                    val rol = snapshots.documents[0].getString("rol") ?: "usuario"
                    onRolChange(rol)
                } else {
                    onRolChange("usuario") // Rol por defecto si no se encuentra.
                }
            }
    }

    /**
     * Escucha si el usuario (responsable) tiene reportes que no estén "Cerrados".
     * @param userEmail Email del usuario a verificar.
     * @param onLevantamientoChange Callback que devuelve true si hay reportes pendientes.
     */
    fun escucharLevantamiento(userEmail: String, onLevantamientoChange: (Boolean) -> Unit) {
        // Remueve el listener anterior.
        reportesListener?.remove()

        if (userEmail.isEmpty()) {
            onLevantamientoChange(false)
            return
        }

        // Escucha cambios en los reportes asignados al responsable.
        reportesListener = db.collection("reportes")
            .whereEqualTo("responsable", userEmail)
            .addSnapshotListener { snapshots, _ ->
                val hayNoCerrado = snapshots?.any { doc ->
                    doc.getString("estado") != "Cerrado"
                } ?: false
                onLevantamientoChange(hayNoCerrado)
            }
    }

    /**
     * Limpia y remueve todos los listeners activos para prevenir fugas de memoria.
     */
    fun limpiarListeners() {
        rolListener?.remove()
        reportesListener?.remove()
        rolListener = null
        reportesListener = null
    }
}