package com.example.reportes.data.repository.admin

import com.example.reportes.data.Remote.FirebaseService
import com.google.firebase.firestore.DocumentSnapshot

class InformacionRepository {
    private val db = FirebaseService.db

    /**
     * Carga los datos del reporte y su correspondiente documento de cierre desde Firestore.
     */
    fun cargarDatos(
        reporteId: String,
        onSuccess: (DocumentSnapshot, DocumentSnapshot?) -> Unit,
        onFailure: (String) -> Unit
    ) {
        db.collection("reportes").document(reporteId)
            .get()
            .addOnSuccessListener { reporteDoc ->
                if (reporteDoc.exists()) {
                    // Busca el documento de cierre asociado al ID del reporte
                    db.collection("reportesCerrados")
                        .whereEqualTo("reporteId", reporteId)
                        .get()
                        .addOnSuccessListener { cerrados ->
                            val cerradoDoc = if (!cerrados.isEmpty) cerrados.documents[0] else null
                            onSuccess(reporteDoc, cerradoDoc)
                        }
                        .addOnFailureListener { onFailure("Error al cargar el cierre del reporte") }
                } else {
                    onFailure("El reporte no existe.")
                }
            }
            .addOnFailureListener { onFailure("Error al cargar la información del reporte") }
    }

    /**
     * Actualiza el estado del cierre a "aceptado" y el estado del reporte a "Cerrado".
     */
    fun aceptarCierre(
        cerradoId: String,
        reporteId: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        db.collection("reportesCerrados").document(cerradoId)
            .update("estadoCierre", "aceptado")
            .addOnSuccessListener {
                db.collection("reportes").document(reporteId)
                    .update("estado", "Cerrado")
                    .addOnSuccessListener { onSuccess() }
                    .addOnFailureListener { onFailure("Error al actualizar el estado del reporte") }
            }
            .addOnFailureListener { onFailure("Error al aceptar el cierre") }
    }

    /**
     * Actualiza el estado del cierre a "rechazado" y revierte el estado del reporte a "Asignado",
     * opcionalmente actualizando la fecha límite.
     */
    fun rechazarCierre(
        cerradoId: String,
        reporteId: String,
        motivo: String,
        fechaRechazo: String,
        nuevaFechaLimite: String?,
        seCambioFecha: Boolean,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        // Datos a actualizar en el documento de cierre
        val updateCerradoData = hashMapOf(
            "estadoCierre" to "rechazado",
            "motivo" to motivo,
            "fecharechazo" to fechaRechazo
        )

        db.collection("reportesCerrados").document(cerradoId)
            .update(updateCerradoData as Map<String, Any>)
            .addOnSuccessListener {
                // Datos a actualizar en el documento del reporte principal
                val updateReporteData = mutableMapOf<String, Any>("estado" to "Asignado")
                if (seCambioFecha && nuevaFechaLimite != null) {
                    updateReporteData["fechaLimite"] = nuevaFechaLimite
                }

                db.collection("reportes").document(reporteId)
                    .update(updateReporteData)
                    .addOnSuccessListener { onSuccess() }
                    .addOnFailureListener { onFailure("Error al actualizar el estado del reporte") }
            }
            .addOnFailureListener { onFailure("Error al rechazar el cierre") }
    }
}