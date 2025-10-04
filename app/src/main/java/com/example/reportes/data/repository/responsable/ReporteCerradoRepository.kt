package com.example.reportes.data.repository.responsable

import android.net.Uri
import com.example.reportes.data.Remote.FirebaseService
import com.google.firebase.storage.FirebaseStorage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Repositorio para gestionar las operaciones de los reportes cerrados en Firebase.
 */
class ReporteCerradoRepository {

    /**
     * Sube la imagen de evidencia a Firebase Storage y luego guarda los detalles en Firestore.
     */
    fun guardarReporteCerrado(
        reporteId: String,
        comentario: String,
        imageUri: Uri,
        imageName: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val storageRef = FirebaseStorage.getInstance().reference.child("evidencias/$imageName")

        storageRef.putFile(imageUri)
            .continueWithTask { task ->
                if (!task.isSuccessful) {
                    task.exception?.let { throw it }
                }
                storageRef.downloadUrl
            }
            .addOnSuccessListener { uri ->
                guardarEnFirestore(reporteId, comentario, uri.toString(), onSuccess, onFailure)
            }
            .addOnFailureListener(onFailure)
    }

    /**
     * Guarda o actualiza la información del reporte cerrado en la colección 'reportesCerrados' de Firestore.
     * Si ya existe un documento para el reporteId, lo actualiza. Si no, crea uno nuevo.
     */
    private fun guardarEnFirestore(
        reporteId: String,
        comentario: String,
        imageUrl: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val db = FirebaseService.db
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val fechaStr = sdf.format(Date())

        db.collection("reportesCerrados").whereEqualTo("reporteId", reporteId).get()
            .addOnSuccessListener { docs ->
                if (!docs.isEmpty) {
                    // El documento existe, se actualiza
                    val updateData = mapOf(
                        "comentario" to comentario,
                        "fechalevantamiento" to fechaStr,
                        "imagenUrl" to imageUrl,
                        "estadoCierre" to "pendiente"
                    )
                    docs.documents[0].reference.update(updateData)
                        .addOnSuccessListener { onSuccess() }
                        .addOnFailureListener(onFailure)
                } else {
                    // El documento no existe, se crea uno nuevo
                    val cierreData = hashMapOf(
                        "reporteId" to reporteId,
                        "comentario" to comentario,
                        "fechalevantamiento" to fechaStr,
                        "imagenUrl" to imageUrl,
                        "estadoCierre" to "pendiente"
                    )
                    db.collection("reportesCerrados").add(cierreData)
                        .addOnSuccessListener { onSuccess() }
                        .addOnFailureListener(onFailure)
                }
            }
            .addOnFailureListener(onFailure)
    }
}