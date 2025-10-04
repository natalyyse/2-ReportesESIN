package com.example.reportes.data.repository

import com.example.reportes.data.Remote.FirebaseService
import com.example.reportes.data.model.Reporte
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ListenerRegistration

class ReportesEstadoRepository {

    private var firestoreListener: ListenerRegistration? = null
    private var cerradosListener: ListenerRegistration? = null

    fun obtenerRolUsuario(email: String?, onResult: (String) -> Unit) {
        if (email == null) {
            onResult("usuario")
            return
        }
        FirebaseService.db.collection("usuarios")
            .whereEqualTo("email", email)
            .get()
            .addOnSuccessListener { docs ->
                val rol = docs.documents.firstOrNull()?.getString("rol") ?: "usuario"
                onResult(rol)
            }
            .addOnFailureListener { onResult("usuario") }
    }

    fun observarReportes(
        rolUsuario: String,
        email: String?,
        onReportesActualizados: (List<Reporte>, Set<String>) -> Unit
    ) {
        val db = FirebaseService.db
        if (rolUsuario == "admin") {
            firestoreListener = db.collection("reportes").addSnapshotListener { result, error ->
                if (error != null) return@addSnapshotListener
                val listaReportes = mutableListOf<Reporte>()
                val ids = mutableListOf<String>()
                result?.forEach { doc ->
                    ids.add(doc.id)
                    listaReportes.add(docToReporte(doc))
                }
                if (ids.isNotEmpty()) {
                    cerradosListener?.remove()
                    cerradosListener = db.collection("reportesCerrados")
                        .whereIn("reporteId", ids)
                        .addSnapshotListener { cerradosResult, cerradosError ->
                            if (cerradosError != null) return@addSnapshotListener
                            val pendientesCerradosIds = cerradosResult?.filter {
                                (it.getString("estadoCierre") ?: "") == "pendiente"
                            }?.mapNotNull { it.getString("reporteId") }?.toSet() ?: emptySet()
                            onReportesActualizados(listaReportes, pendientesCerradosIds)
                        }
                } else {
                    onReportesActualizados(listaReportes, emptySet())
                }
            }
        } else {
            firestoreListener = db.collection("reportes")
                .whereEqualTo("responsable", email)
                .addSnapshotListener { result, error ->
                    if (error != null) {
                        onReportesActualizados(emptyList(), emptySet())
                        return@addSnapshotListener
                    }
                    val reportesMap = mutableMapOf<String, Reporte>()
                    val reporteIds = mutableListOf<String>()
                    result?.forEach { doc ->
                        reporteIds.add(doc.id)
                        reportesMap[doc.id] = docToReporte(doc)
                    }
                    if (reporteIds.isEmpty()) {
                        onReportesActualizados(emptyList(), emptySet())
                        return@addSnapshotListener
                    }
                    cerradosListener?.remove()
                    cerradosListener = db.collection("reportesCerrados")
                        .whereIn("reporteId", reporteIds)
                        .addSnapshotListener { cerradosResult, cerradosError ->
                            if (cerradosError != null) return@addSnapshotListener
                            val pendientesIds = cerradosResult?.filter {
                                (it.getString("estadoCierre") ?: "") == "pendiente"
                            }?.mapNotNull { it.getString("reporteId") }?.toSet() ?: emptySet()
                            val listaReportes = reportesMap.map { (id, reporte) ->
                                if (pendientesIds.contains(id)) {
                                    if (reporte.estado.equals("Cerrado parcial", true)) {
                                        reporte.copy(estado = "Cerrado parcial")
                                    } else {
                                        reporte.copy(estado = "Pendiente")
                                    }
                                } else {
                                    reporte
                                }
                            }
                            onReportesActualizados(listaReportes, pendientesIds)
                        }
                }
        }
    }

    fun limpiarListeners() {
        firestoreListener?.remove()
        cerradosListener?.remove()
        firestoreListener = null
        cerradosListener = null
    }

    private fun docToReporte(doc: com.google.firebase.firestore.DocumentSnapshot): Reporte {
        return Reporte(
            id = doc.id,
            tipo = doc.getString("tipo") ?: "",
            nivelRiesgo = doc.getString("nivelRiesgo") ?: "",
            descripcion = doc.getString("descripcion") ?: "",
            lugar = doc.getString("lugar") ?: "",
            fechacreacion = doc.getString("fechacreacion") ?: "",
            reportante = doc.getString("reportante") ?: "",
            imagenUrl = doc.getString("imagenUrl") ?: "",
            estado = doc.getString("estado") ?: "",
            fechaLimite = doc.getString("fechaLimite") ?: "",
            responsable = doc.getString("responsable") ?: ""
        )
    }
}