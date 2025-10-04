package com.example.reportes.data.repository.responsable

import com.example.reportes.data.Remote.FirebaseService
import com.example.reportes.data.model.ReporteCerrado
import com.google.firebase.firestore.FirebaseFirestore

class DetalleReporteARepository {

    // --- Constantes ---
    companion object {
        private const val COLLECTION_REPORTES_CERRADOS = "reportesCerrados"
        private const val FIELD_REPORTE_ID = "reporteId"
    }

    // --- Instancia de Firestore ---
    private val db: FirebaseFirestore = FirebaseService.db

    /**
     * Obtiene el historial de cierre de un reporte desde Firestore.
     * Busca en la colección 'reportesCerrados' un documento que coincida con el reporteId.
     *
     * @param reporteId El ID del reporte para el cual se busca el historial.
     * @param callback Una función de retorno que se invoca con el resultado de la operación.
     *                 Devuelve Result.success con el objeto ReporteCerrado o null si no se encuentra.
     *                 Devuelve Result.failure con una excepción si ocurre un error.
     */
    fun getHistorialCierre(reporteId: String, callback: (Result<ReporteCerrado?>) -> Unit) {
        db.collection(COLLECTION_REPORTES_CERRADOS)
            .whereEqualTo(FIELD_REPORTE_ID, reporteId)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    // Si se encuentra un documento, se convierte al modelo ReporteCerrado.
                    val doc = documents.documents[0]
                    val reporteCerrado = doc.toObject(ReporteCerrado::class.java)
                    callback(Result.success(reporteCerrado))
                } else {
                    // Si no hay documentos, significa que no hay historial.
                    callback(Result.success(null))
                }
            }
            .addOnFailureListener { exception ->
                // Si la consulta falla, se devuelve el error.
                callback(Result.failure(exception))
            }
    }
}