package com.example.reportes.data.repository.admin

import com.example.reportes.data.Remote.FirebaseService
import com.google.firebase.firestore.FirebaseFirestore

class EditarRepository {

    private val db: FirebaseFirestore = FirebaseService.db

    fun cargarDatosReporte(reporteId: String, callback: (Result<Map<String, String>>) -> Unit) {
        db.collection("reportes").document(reporteId).get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    val datos = mapOf(
                        "tipo" to (doc.getString("tipo") ?: ""),
                        "responsable" to (doc.getString("responsable") ?: ""),
                        "fechaAsignacion" to (doc.getString("fechaAsignacion") ?: ""),
                        "fechaLimite" to (doc.getString("fechaLimite") ?: "")
                    )
                    callback(Result.success(datos))
                } else {
                    callback(Result.failure(Exception("El documento no existe.")))
                }
            }
            .addOnFailureListener { e ->
                callback(Result.failure(e))
            }
    }

    fun actualizarFechaLimite(
        reporteId: String,
        fechaLimiteStr: String,
        esReporteCerradoParcial: Boolean,
        callback: (Result<Unit>) -> Unit
    ) {
        val actualizaciones = mutableMapOf<String, Any>(
            "fechaLimite" to fechaLimiteStr
        )
        if (esReporteCerradoParcial) {
            actualizaciones["estado"] = "Asignado"
        }

        db.collection("reportes").document(reporteId).update(actualizaciones)
            .addOnSuccessListener {
                if (esReporteCerradoParcial) {
                    db.collection("reportesCerrados")
                        .whereEqualTo("reporteId", reporteId)
                        .get()
                        .addOnSuccessListener { cerrados ->
                            val batch = db.batch()
                            for (doc in cerrados) {
                                batch.delete(doc.reference)
                            }
                            batch.commit()
                                .addOnSuccessListener { callback(Result.success(Unit)) }
                                .addOnFailureListener { e -> callback(Result.failure(e)) }
                        }
                        .addOnFailureListener { e -> callback(Result.failure(e)) }
                } else {
                    callback(Result.success(Unit))
                }
            }
            .addOnFailureListener { e ->
                callback(Result.failure(e))
            }
    }
}