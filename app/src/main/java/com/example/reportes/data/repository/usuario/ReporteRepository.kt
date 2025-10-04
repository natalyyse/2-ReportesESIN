package com.example.reportes.data.repository.usuario

import android.net.Uri
import com.example.reportes.data.Remote.FirebaseService
import com.example.reportes.data.model.Reporte
import com.google.firebase.storage.FirebaseStorage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class ReporteRepository {
    private val db = FirebaseService.db
    private val auth = FirebaseService.auth
    private val storage = FirebaseStorage.getInstance() // Puedes crear un StorageService si quieres

    fun subirImagen(
        uri: Uri,
        onSuccess: (String) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val storageRef = storage.reference.child("reportes/${UUID.randomUUID()}.jpg")
        storageRef.putFile(uri)
            .addOnSuccessListener {
                storageRef.downloadUrl.addOnSuccessListener { downloadUrl ->
                    onSuccess(downloadUrl.toString())
                }
            }
            .addOnFailureListener { exception ->
                onFailure(exception)
            }
    }

    fun guardarReporte(
        reporte: Reporte,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        // Solo incluir campos no vacíos
        val data = mutableMapOf<String, Any>()
        if (reporte.tipo.isNotEmpty()) data["tipo"] = reporte.tipo
        if (reporte.descripcion.isNotEmpty()) data["descripcion"] = reporte.descripcion
        if (reporte.lugar.isNotEmpty()) data["lugar"] = reporte.lugar
        if (reporte.reportante.isNotEmpty()) data["reportante"] = reporte.reportante
        if (reporte.imagenUrl.isNotEmpty()) data["imagenUrl"] = reporte.imagenUrl
        if (reporte.fechacreacion.isNotEmpty()) data["fechacreacion"] = reporte.fechacreacion
        if (reporte.estado.isNotEmpty()) data["estado"] = reporte.estado
        if (reporte.id.isNotEmpty()) data["id"] = reporte.id
        if (reporte.fechaLimite.isNotEmpty()) data["fechaLimite"] = reporte.fechaLimite
        if (reporte.nivelRiesgo.isNotEmpty()) data["nivelRiesgo"] = reporte.nivelRiesgo
        if (reporte.responsable.isNotEmpty()) data["responsable"] = reporte.responsable

        db.collection("reportes")
            .add(data)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { exception -> onFailure(exception) }
    }

    fun crearReporte(
        tipo: String,
        descripcion: String,
        lugar: String,
        reportante: String,
        imagenUrl: String // Se elimina el '?' para que el parámetro sea obligatorio
    ): Reporte {
        val uid = auth.currentUser?.uid ?: ""
        val fechaActual = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
        return Reporte(
            tipo = tipo,
            descripcion = descripcion,
            lugar = lugar,
            reportante = reportante,
            imagenUrl = imagenUrl, // Ya no se necesita el operador elvis '?: ""'
            fechacreacion = fechaActual,
            estado = "Pendiente",
            id = uid
        )
    }
}