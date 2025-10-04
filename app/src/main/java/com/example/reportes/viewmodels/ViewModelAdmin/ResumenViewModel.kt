package com.example.reportes.viewmodels.ViewModelAdmin

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.reportes.data.model.Reporte
import com.example.reportes.data.model.ReporteCerrado
import com.example.reportes.data.repository.admin.ResumenRepository
import com.google.firebase.firestore.ListenerRegistration
import java.text.SimpleDateFormat
import java.util.Date

/**
 * ViewModel para la gestión de datos de resumen de reportes.
 * Maneja la lógica de negocio relacionada con los reportes y su visualización.
 */
class ResumenViewModel : ViewModel() {

    // SECCIÓN: PROPIEDADES Y DEPENDENCIAS
    private val repository = ResumenRepository()
    private var reportesListener: ListenerRegistration? = null

    // SECCIÓN: LIVEDATA
    private val _allReportes = MutableLiveData<List<Reporte>>()
    val allReportes: LiveData<List<Reporte>> = _allReportes

    private val _reportesFiltrados = MutableLiveData<List<Reporte>>()
    val reportesFiltrados: LiveData<List<Reporte>> = _reportesFiltrados

    private val _datosExportar = MutableLiveData<List<Pair<Reporte, ReporteCerrado?>>>()
    val datosExportar: LiveData<List<Pair<Reporte, ReporteCerrado?>>> = _datosExportar

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    // SECCIÓN: INICIALIZACIÓN
    init {
        iniciarListenerReportes()
    }

    /**
     * Inicia el listener para obtener actualizaciones en tiempo real de los reportes
     */
    private fun iniciarListenerReportes() {
        reportesListener = repository.getReportesListener(
            onUpdate = { reportes -> _allReportes.value = reportes },
            onError = { exception -> _error.value = "Error al escuchar cambios: ${exception.message}" }
        )
    }

    // SECCIÓN: MÉTODOS PÚBLICOS
    /**
     * Obtiene las fechas extremas (menor y mayor) de los reportes
     * @param reportes Lista de reportes para obtener las fechas
     * @return Par con fecha inicial y fecha final formateadas
     */
    fun getFechasIniciales(reportes: List<Reporte>): Pair<String, String> {
        val sdf = SimpleDateFormat("dd/MM/yyyy")
        if (reportes.isEmpty()) {
            val fechaActualStr = sdf.format(Date())
            return Pair(fechaActualStr, fechaActualStr)
        }
        val fechas = reportes.mapNotNull { try { sdf.parse(it.fechacreacion) } catch (_: Exception) { null } }.sorted()
        val primeraFecha = fechas.firstOrNull() ?: Date()
        val ultimaFecha = fechas.lastOrNull() ?: primeraFecha
        return Pair(sdf.format(primeraFecha), sdf.format(ultimaFecha))
    }

    /**
     * Actualiza los datos para los gráficos según los filtros aplicados
     */
    fun actualizarGraficos(fechaDesde: String, fechaHasta: String, tipo: String) {
        repository.getReportesFiltrados(fechaDesde, fechaHasta, tipo,
            onSuccess = { reportes -> _reportesFiltrados.value = reportes },
            onFailure = { exception -> _error.value = "Error al filtrar reportes: ${exception.message}" }
        )
    }

    /**
     * Prepara los datos para exportar a Excel según el rango de fechas
     */
    fun prepararDatosExportar(fechaDesde: String, fechaHasta: String) {
        repository.getDatosParaExportar(fechaDesde, fechaHasta,
            onSuccess = { datos -> _datosExportar.value = datos },
            onFailure = { exception -> _error.value = "Error al obtener datos para exportar: ${exception.message}" }
        )
    }

    // SECCIÓN: LIMPIEZA
    override fun onCleared() {
        super.onCleared()
        reportesListener?.remove()
    }
}