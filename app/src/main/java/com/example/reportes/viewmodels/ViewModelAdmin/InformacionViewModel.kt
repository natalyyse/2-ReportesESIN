package com.example.reportes.viewmodels.ViewModelAdmin

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.reportes.data.repository.admin.InformacionRepository
import com.google.firebase.firestore.DocumentSnapshot

class InformacionViewModel : ViewModel() {
    private val repository = InformacionRepository()

    private val _reporteDoc = MutableLiveData<DocumentSnapshot>()
    val reporteDoc: LiveData<DocumentSnapshot> = _reporteDoc

    private val _cerradoDoc = MutableLiveData<DocumentSnapshot?>()
    val cerradoDoc: LiveData<DocumentSnapshot?> = _cerradoDoc

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    private val _accionExitosa = MutableLiveData<Boolean>()
    val accionExitosa: LiveData<Boolean> = _accionExitosa

    fun cargarDatos(reporteId: String) {
        repository.cargarDatos(reporteId,
            onSuccess = { reporte, cerrado ->
                _reporteDoc.value = reporte
                _cerradoDoc.value = cerrado
            },
            onFailure = { errorMsg ->
                _error.value = errorMsg
            }
        )
    }

    fun aceptarCierre(cerradoId: String, reporteId: String) {
        repository.aceptarCierre(cerradoId, reporteId,
            onSuccess = { _accionExitosa.value = true },
            onFailure = { errorMsg -> _error.value = errorMsg }
        )
    }

    fun rechazarCierre(
        cerradoId: String,
        reporteId: String,
        motivo: String,
        fechaRechazo: String,
        nuevaFechaLimite: String?,
        seCambioFecha: Boolean
    ) {
        repository.rechazarCierre(cerradoId, reporteId, motivo, fechaRechazo, nuevaFechaLimite, seCambioFecha,
            onSuccess = { _accionExitosa.value = true },
            onFailure = { errorMsg -> _error.value = errorMsg }
        )
    }
}