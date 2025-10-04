package com.example.reportes.data.repository.admin

import com.example.reportes.data.Remote.FirebaseService
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.QuerySnapshot

class DetallesRepository {

    private val db = FirebaseService.db

    /**
     * Establece un listener en tiempo real para un documento de reporte específico en Firestore.
     * Notifica los cambios a través del callback.
     */
    fun escucharDetallesReporte(reporteId: String, callback: (Result<DocumentSnapshot>) -> Unit): ListenerRegistration {
        return db.collection("reportes").document(reporteId)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    callback(Result.failure(e))
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    callback(Result.success(snapshot))
                } else {
                    callback(Result.failure(Exception("Reporte no encontrado")))
                }
            }
    }

    /**
     * Obtiene la información de cierre de un reporte desde la colección 'reportesCerrados'.
     */
    fun obtenerReporteCerrado(reporteId: String, callback: (Result<QuerySnapshot>) -> Unit) {
        db.collection("reportesCerrados").whereEqualTo("reporteId", reporteId).get()
            .addOnSuccessListener { querySnapshot ->
                callback(Result.success(querySnapshot))
            }
            .addOnFailureListener { e ->
                callback(Result.failure(e))
            }
    }

    /**
     * Carga los detalles de un reporte una sola vez desde Firestore.
     */
    fun cargarDetallesReporte(reporteId: String, callback: (Result<DocumentSnapshot>) -> Unit) {
        db.collection("reportes").document(reporteId).get()
            .addOnSuccessListener { documentSnapshot ->
                if (documentSnapshot != null && documentSnapshot.exists()) {
                    callback(Result.success(documentSnapshot))
                } else {
                    callback(Result.failure(Exception("Reporte no encontrado")))
                }
            }
            .addOnFailureListener { e ->
                callback(Result.failure(e))
            }
    }
}