package com.example.reportes.viewmodels.ViewModelResponsable

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.reportes.data.repository.responsable.ReporteCerradoRepository

/**
 * ViewModel para la pantalla de cierre de reportes.
 * Gestiona la lógica de negocio y la comunicación con el repositorio.
 */
class ReporteCerradoViewModel : ViewModel() {

    private val repository = ReporteCerradoRepository()

    // LiveData para comunicar el resultado de la operación de guardado a la UI.
    private val _cierreResult = MutableLiveData<Result<Unit>>()
    val cierreResult: LiveData<Result<Unit>> = _cierreResult

    // LiveData para controlar la visibilidad de la barra de progreso.
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    /**
     * Valida las entradas del usuario y llama al repositorio para guardar el reporte cerrado.
     */
    fun guardarReporte(
        reporteId: String,
        comentario: String,
        imageUri: Uri?,
        imageName: String
    ) {
        // Validaciones de entrada
        if (comentario.isBlank()) {
            _cierreResult.value = Result.failure(Exception("Completa el comentario"))
            return
        }
        if (imageUri == null) {
            _cierreResult.value = Result.failure(Exception("Selecciona una imagen"))
            return
        }

        _isLoading.value = true

        // Llamada al repositorio para guardar los datos
        repository.guardarReporteCerrado(reporteId, comentario, imageUri, imageName,
            onSuccess = {
                _isLoading.value = false
                _cierreResult.value = Result.success(Unit)
            },
            onFailure = { exception ->
                _isLoading.value = false
                _cierreResult.value = Result.failure(exception)
            }
        )
    }
}