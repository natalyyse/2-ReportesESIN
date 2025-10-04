package com.example.reportes.viewmodels.ViewModelResponsable

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.reportes.data.model.ReporteCerrado
import com.example.reportes.data.repository.responsable.DetalleReporteARepository

class DetalleReporteAViewModel : ViewModel() {

    // --- Repositorio ---
    private val repository = DetalleReporteARepository()

    // --- LiveData para el historial de cierre ---
    // El valor será un objeto ReporteCerrado si existe, o null si no hay historial.
    private val _historialCierre = MutableLiveData<ReporteCerrado?>()
    val historialCierre: LiveData<ReporteCerrado?> get() = _historialCierre

    // --- LiveData para errores ---
    private val _error = MutableLiveData<String>()
    val error: LiveData<String> get() = _error

    /**
     * Carga el historial de cierre de un reporte específico desde el repositorio.
     * @param reporteId El ID del reporte a consultar.
     */
    fun cargarHistorial(reporteId: String) {
        repository.getHistorialCierre(reporteId) { result ->
            result.onSuccess { reporteCerrado ->
                // Publica el resultado (puede ser el objeto o null) en el LiveData.
                // La UI se encargará de interpretar un valor nulo como "sin historial".
                _historialCierre.postValue(reporteCerrado)
            }.onFailure {
                // En caso de fallo, publica un mensaje de error.
                _error.postValue("Error al cargar el historial")
            }
        }
    }
}