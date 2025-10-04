package com.example.reportes.data.repository.admin

import com.example.reportes.data.model.Reporte
import com.example.reportes.data.model.ReporteCerrado
import com.example.reportes.data.Remote.FirebaseService
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.Date

/**
 * Repositorio para manejar los datos de reportes para la funcionalidad de resumen
 * Se encarga de las operaciones contra Firestore
 */
class ResumenRepository {

    // SECCIÓN: PROPIEDADES
    private val db = FirebaseService.db
    private val reportesCollection = db.collection("reportes")
    private val reportesCerradosCollection = db.collection("reportesCerrados")

    // SECCIÓN: MÉTODOS DE ACCESO A DATOS
    
    /**
     * Configura un listener en tiempo real para la colección de reportes
     * @return La referencia al listener para poder eliminarlo posteriormente
     */
    fun getReportesListener(onUpdate: (List<Reporte>) -> Unit, onError: (Exception) -> Unit): ListenerRegistration {
        return reportesCollection.addSnapshotListener { snapshots, e ->
            if (e != null) {
                onError(e)
                return@addSnapshotListener
            }
            val reportes = snapshots?.documents?.mapNotNull {
                it.toObject(Reporte::class.java)?.copy(id = it.id)
            } ?: emptyList()
            onUpdate(reportes)
        }
    }

    /**
     * Obtiene los reportes filtrados por fecha y tipo
     */
    fun getReportesFiltrados(
        fechaDesde: String,
        fechaHasta: String,
        tipo: String,
        onSuccess: (List<Reporte>) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val sdf = SimpleDateFormat("dd/MM/yyyy")
        val desdeDate = try { sdf.parse(fechaDesde) } catch (e: Exception) { null }
        val hastaDate = try { sdf.parse(fechaHasta) } catch (e: Exception) { null }

        var query: Query = reportesCollection
        if (tipo != "Todos") {
            query = query.whereEqualTo("tipo", tipo)
        }

        query.get()
            .addOnSuccessListener { result ->
                val reportesFiltrados = result.documents.mapNotNull { doc ->
                    val fechaStr = doc.getString("fechacreacion") ?: ""
                    val fechaDoc = try { sdf.parse(fechaStr) } catch (e: Exception) { null }

                    if (fechaDoc != null && desdeDate != null && hastaDate != null &&
                        !fechaDoc.before(desdeDate) && !fechaDoc.after(hastaDate)) {
                        doc.toObject(Reporte::class.java)?.copy(id = doc.id)
                    } else {
                        null
                    }
                }
                onSuccess(reportesFiltrados)
            }
            .addOnFailureListener { exception ->
                onFailure(exception)
            }
    }

    /**
     * Obtiene datos combinados de reportes y reportes cerrados para exportación
     */
    fun getDatosParaExportar(
        fechaDesde: String,
        fechaHasta: String,
        onSuccess: (List<Pair<Reporte, ReporteCerrado?>>) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val sdf = SimpleDateFormat("dd/MM/yyyy")
        val desdeDate = try { sdf.parse(fechaDesde) } catch (e: Exception) { null }
        val hastaDate = try { sdf.parse(fechaHasta) } catch (e: Exception) { null }

        reportesCerradosCollection.get().addOnSuccessListener { cerradosResult ->
            val cerradosMap = cerradosResult.documents.associate {
                it.getString("reporteId") to it.toObject(ReporteCerrado::class.java)
            }

            reportesCollection.get().addOnSuccessListener { reportesResult ->
                val datosCombinados = reportesResult.documents.mapNotNull { doc ->
                    val fechaStr = doc.getString("fechacreacion") ?: ""
                    val fechaDoc = try { sdf.parse(fechaStr) } catch (e: Exception) { null }

                    if (fechaDoc != null && desdeDate != null && hastaDate != null &&
                        !fechaDoc.before(desdeDate) && !fechaDoc.after(hastaDate)) {
                        val reporte = doc.toObject(Reporte::class.java)?.copy(id = doc.id)
                        if (reporte != null) {
                            val reporteCerrado = cerradosMap[reporte.id]
                            Pair(reporte, reporteCerrado)
                        } else {
                            null
                        }
                    } else {
                        null
                    }
                }
                onSuccess(datosCombinados)
            }.addOnFailureListener(onFailure)
        }.addOnFailureListener(onFailure)
    }
}