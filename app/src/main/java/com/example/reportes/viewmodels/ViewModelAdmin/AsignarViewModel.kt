package com.example.reportes.viewmodels.ViewModelAdmin

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.reportes.data.repository.admin.AsignarRepository

class AsignarViewModel(
    private val repository: AsignarRepository = AsignarRepository()
) : ViewModel() {

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> get() = _loading

    private val _asignacionExitosa = MutableLiveData<Map<String, Any>?>()
    val asignacionExitosa: LiveData<Map<String, Any>?> get() = _asignacionExitosa

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> get() = _error

    fun asignarReporte(
        reporteId: String,
        responsable: String,
        fechaLimite: String,
        nivelRiesgo: String
    ) {
        _loading.value = true
        repository.asignarReporte(
            reporteId,
            responsable,
            fechaLimite,
            nivelRiesgo,
            onSuccess = { data ->
                _loading.postValue(false)
                _asignacionExitosa.postValue(data)
            },
            onError = { e ->
                _loading.postValue(false)
                _error.postValue(e.message ?: "Error desconocido")
            }
        )
    }

    fun limpiarEstado() {
        _asignacionExitosa.value = null
        _error.value = null
    }
}