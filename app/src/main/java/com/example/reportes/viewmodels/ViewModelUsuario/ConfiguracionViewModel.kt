package com.example.reportes.viewmodels.ViewModelUsuario

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.reportes.data.repository.usuario.ConfiguracionRepository

class ConfiguracionViewModel : ViewModel() {
    private val repository = ConfiguracionRepository()
    private val _correo = MutableLiveData<String>()
    val correo: LiveData<String> = _correo

    fun cargarCorreo() {
        repository.obtenerCorreoUsuario(
            onSuccess = { correo -> _correo.value = correo },
            onFailure = { correo -> _correo.value = correo }
        )
    }
}