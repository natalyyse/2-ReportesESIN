package com.example.reportes.viewmodels.ViewModelAdmin

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.reportes.data.repository.admin.EditarRepository

class EditarViewModel : ViewModel() {

    private val repository = EditarRepository()

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> get() = _loading

    private val _success = MutableLiveData<Boolean>()
    val success: LiveData<Boolean> get() = _success

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> get() = _error

    private val _datosReporte = MutableLiveData<Map<String, String>>()
    val datosReporte: LiveData<Map<String, String>> get() = _datosReporte

    fun cargarDatosReporte(reporteId: String) {
        _loading.value = true
        repository.cargarDatosReporte(reporteId) { result ->
            result.onSuccess { datos ->
                _datosReporte.postValue(datos)
            }.onFailure { e ->
                _error.postValue("Error al cargar datos: ${e.message}")
            }
            _loading.postValue(false)
        }
    }

    fun actualizarFechaLimite(
        reporteId: String,
        fechaLimiteStr: String,
        esReporteCerradoParcial: Boolean
    ) {
        _loading.value = true
        repository.actualizarFechaLimite(reporteId, fechaLimiteStr, esReporteCerradoParcial) { result ->
            result.onSuccess {
                _success.postValue(true)
            }.onFailure { e ->
                _error.postValue("Error al actualizar reporte: ${e.message}")
            }
            _loading.postValue(false)
        }
    }

    fun limpiarEstado() {
        _success.value = false
        _error.value = null
    }
}