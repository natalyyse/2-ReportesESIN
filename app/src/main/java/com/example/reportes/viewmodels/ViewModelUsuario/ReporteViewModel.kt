package com.example.reportes.viewmodels.ViewModelUsuario

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.reportes.data.repository.usuario.ReporteRepository

class ReporteViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ReporteRepository()

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading

    private val _mensaje = MutableLiveData<String>()
    val mensaje: LiveData<String> = _mensaje

    private val _reporteGuardado = MutableLiveData<Boolean>()
    val reporteGuardado: LiveData<Boolean> = _reporteGuardado

    fun enviarReporte(
        tipo: String,
        descripcion: String,
        lugar: String,
        reportante: String,
        imagenUri: Uri?
    ) {
        _loading.value = true
        if (imagenUri != null) {
            repository.subirImagen(imagenUri, { url ->
                guardarReporte(tipo, descripcion, lugar, reportante, url)
            }, { error ->
                _loading.value = false
                _mensaje.value = "Error al subir la imagen"
            })
        } else {
            // Si no hay imagen, se muestra un error y se detiene el proceso.
            _loading.value = false
            _mensaje.value = "Es obligatorio adjuntar una imagen"
        }
    }

    private fun guardarReporte(
        tipo: String,
        descripcion: String,
        lugar: String,
        reportante: String,
        imagenUrl: String // Se elimina el '?' para que el parámetro sea obligatorio
    ) {
        // Ahora 'imagenUrl' es un String no nulo, coincidiendo con 'crearReporte'
        val reporte = repository.crearReporte(tipo, descripcion, lugar, reportante, imagenUrl)
        repository.guardarReporte(reporte, {
            _loading.value = false
            _mensaje.value = "Reporte enviado correctamente"
            _reporteGuardado.value = true
        }, {
            _loading.value = false
            _mensaje.value = "Error al guardar el reporte"
        })
    }
}