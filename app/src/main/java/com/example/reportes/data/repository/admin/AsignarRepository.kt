package com.example.reportes.data.repository.admin

import com.example.reportes.data.Remote.FirebaseService
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AsignarRepository(private val db: FirebaseFirestore = FirebaseService.db) {

    fun asignarReporte(
        reporteId: String,
        responsable: String,
        fechaLimite: String, // Recibe como "dd/MM/yyyy"
        nivelRiesgo: String, // <-- Nuevo parámetro
        onSuccess: (Map<String, Any>) -> Unit,
        onError: (Exception) -> Unit
    ) {
        // Buscar usuario por email antes de asignar
        db.collection("usuarios")
            .whereEqualTo("email", responsable)
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (!querySnapshot.isEmpty) {
                    val userDoc = querySnapshot.documents[0]
                    val currentRol = userDoc.getString("rol")
                    if (currentRol == "admin") {
                        onError(Exception("No puedes ser responsable"))
                        return@addOnSuccessListener
                    }
                    // ...continúa flujo normal...
                    db.collection("reportes").document(reporteId)
                        .get()
                        .addOnSuccessListener { document ->
                            if (document.exists()) {
                                val fechaAsignacionStr = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(
                                    Date()
                                )
                                val updates = hashMapOf<String, Any>(
                                    "estado" to "Asignado",
                                    "responsable" to responsable,
                                    "fechaLimite" to fechaLimite,
                                    "fechaAsignacion" to fechaAsignacionStr,
                                    "nivelRiesgo" to nivelRiesgo // <-- Nuevo campo
                                )
                                db.collection("reportes").document(reporteId)
                                    .update(updates)
                                    .addOnSuccessListener {
                                        if (currentRol != "responsable") {
                                            db.collection("usuarios").document(userDoc.id)
                                                .update("rol", "responsable")
                                        }
                                        db.collection("reportes").document(reporteId)
                                            .get()
                                            .addOnSuccessListener { updatedDocument ->
                                                onSuccess(updatedDocument.data ?: emptyMap())
                                            }
                                            .addOnFailureListener { e -> onError(e) }
                                    }
                                    .addOnFailureListener { e -> onError(e) }
                            } else {
                                onError(Exception("Reporte no encontrado"))
                            }
                        }
                        .addOnFailureListener { e -> onError(e) }
                } else {
                    onError(Exception("Usuario no encontrado"))
                }
            }
            .addOnFailureListener { e -> onError(e) }
    }
}