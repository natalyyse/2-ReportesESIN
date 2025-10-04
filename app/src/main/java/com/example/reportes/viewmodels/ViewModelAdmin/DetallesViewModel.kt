package com.example.reportes.viewmodels.ViewModelAdmin

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.reportes.data.repository.admin.DetallesRepository
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.ListenerRegistration
import java.text.SimpleDateFormat
import java.util.*

class DetallesViewModel : ViewModel() {

    private val repository = DetallesRepository()

    // Define la estructura de datos para el estado de la UI
    data class DetallesUiState(
        val tipo: String = "",
        val descripcion: String = "",
        val lugar: String = "",
        val fecha: String = "",
        val nombre: String = "",
        val imagenUrl: String? = null,
        val estado: String = "",
        val estadoCierre: String? = null,
        val fechaAsignacion: String = "",
        val fechaLimite: String = ""
    )

    // LiveData para el estado de la UI, expuesto a la Activity
    private val _uiState = MutableLiveData<DetallesUiState>()
    val uiState: LiveData<DetallesUiState> = _uiState

    // LiveData para manejar y exponer errores
    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    // Referencia al listener de Firestore para poder removerlo
    private var detallesListener: ListenerRegistration? = null

    /**
     * Inicia la escucha de cambios en tiempo real para un reporte.
     */
    fun escucharDetallesEnTiempoReal(reporteId: String) {
        detallesListener?.remove()
        detallesListener = repository.escucharDetallesReporte(reporteId) { result ->
            result.onSuccess { doc ->
                procesarDocumentoReporte(doc)
            }.onFailure { e ->
                _error.postValue(e.message)
            }
        }
    }

    /**
     * Carga los detalles completos de un reporte una sola vez.
     */
    fun cargarDetallesCompletos(reporteId: String) {
        repository.cargarDetallesReporte(reporteId) { result ->
            result.onSuccess { doc ->
                procesarDocumentoReporte(doc)
            }.onFailure { e ->
                _error.postValue(e.message)
            }
        }
    }

    /**
     * Procesa el DocumentSnapshot de un reporte, extrae los datos y busca su estado de cierre.
     */
    private fun procesarDocumentoReporte(doc: DocumentSnapshot) {
        val reporteId = doc.id
        val tipo = doc.getString("tipo") ?: ""
        val descripcion = doc.getString("descripcion") ?: ""
        val lugar = doc.getString("lugar") ?: ""
        val nombre = doc.getString("reportante") ?: ""
        val fecha = doc.getString("fechacreacion") ?: ""
        val imagenUrl = doc.getString("imagenUrl")
        val estado = doc.getString("estado") ?: ""
        val fechaAsignacion = doc.getString("fechaAsignacion") ?: ""
        val fechaLimite = doc.getString("fechaLimite") ?: ""

        repository.obtenerReporteCerrado(reporteId) { cerradoResult ->
            val estadoCierre = cerradoResult.getOrNull()?.documents?.firstOrNull()?.getString("estadoCierre")
            _uiState.postValue(
                DetallesUiState(
                    tipo, descripcion, lugar, fecha, nombre, imagenUrl,
                    estado, estadoCierre, fechaAsignacion, fechaLimite
                )
            )
        }
    }

    /**
     * Lógica de negocio para determinar si un reporte 'Asignado' puede ser editado.
     * @return Un Par con un booleano (true si se puede editar) y un mensaje de texto.
     */
    fun puedeEditar(fechaAsignacionStr: String, fechaLimiteStr: String): Pair<Boolean, String> {
        try {
            if (fechaAsignacionStr.isEmpty() || fechaLimiteStr.isEmpty()) {
                return Pair(false, "No se puede editar: información de fechas incompleta")
            }
            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val fechaActual = Calendar.getInstance()
            fechaActual.set(Calendar.HOUR_OF_DAY, 0)
            fechaActual.set(Calendar.MINUTE, 0)
            fechaActual.set(Calendar.SECOND, 0)
            fechaActual.set(Calendar.MILLISECOND, 0)
            val fechaAsignacion = Calendar.getInstance()
            fechaAsignacion.time = sdf.parse(fechaAsignacionStr) ?: return Pair(false, "Formato de fecha inválido")
            fechaAsignacion.set(Calendar.HOUR_OF_DAY, 0)
            fechaAsignacion.set(Calendar.MINUTE, 0)
            fechaAsignacion.set(Calendar.SECOND, 0)
            fechaAsignacion.set(Calendar.MILLISECOND, 0)
            val fechaLimite = Calendar.getInstance()
            fechaLimite.time = sdf.parse(fechaLimiteStr) ?: return Pair(false, "Formato de fecha inválido")
            fechaLimite.set(Calendar.HOUR_OF_DAY, 23)
            fechaLimite.set(Calendar.MINUTE, 59)
            fechaLimite.set(Calendar.SECOND, 59)
            fechaLimite.set(Calendar.MILLISECOND, 999)
            val diffMillisTotal = fechaLimite.timeInMillis - fechaAsignacion.timeInMillis
            val diasTotales = (diffMillisTotal / (24 * 60 * 60 * 1000)) + 1
            if (diasTotales <= 2) {
                return Pair(false, "No se puede editar: el plazo es de 2 días o menos")
            }
            if (fechaActual.before(fechaAsignacion)) {
                return Pair(false, "No se puede editar: la fecha de asignación es futura")
            }
            if (fechaActual.after(fechaLimite)) {
                return Pair(false, "No se puede editar: la fecha límite ya pasó")
            }
            val diffMillisRestantes = fechaLimite.timeInMillis - fechaActual.timeInMillis
            val diasRestantes = (diffMillisRestantes / (24 * 60 * 60 * 1000)) + 1
            if (diasRestantes <= 2) {
                return Pair(false, "No se puede editar cuando faltan 2 días para la fecha límite")
            }
            return Pair(true, "")
        } catch (e: Exception) {
            Log.e("DetallesViewModel", "Error al calcular fechas", e)
            return Pair(false, "Error al procesar las fechas: ${e.message}")
        }
    }

    /**
     * Limpia los listeners de Firestore cuando el ViewModel es destruido.
     */
    override fun onCleared() {
        super.onCleared()
        detallesListener?.remove()
    }
}