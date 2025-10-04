package com.example.reportes.viewmodels.ViewModelUsuario

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.reportes.data.repository.usuario.PrincipalRepository
import com.google.firebase.auth.FirebaseAuth

/**
 * ViewModel para la pantalla principal. Gestiona el estado de la UI
 * relacionado con el rol del usuario y la visibilidad de las opciones del menú.
 */
class PrincipalViewModel(application: Application) : AndroidViewModel(application) {

    // Instancia del repositorio para acceder a los datos.
    private val repository = PrincipalRepository()
    private val auth = FirebaseAuth.getInstance()

    // LiveData para el rol del usuario.
    private val _rol = MutableLiveData<String>()
    val rol: LiveData<String> = _rol

    // LiveData para indicar si hay levantamientos pendientes para un "responsable".
    private val _hayLevantamiento = MutableLiveData<Boolean>(false)
    val hayLevantamiento: LiveData<Boolean> = _hayLevantamiento

    /**
     * Inicia la escucha de cambios en el rol del usuario.
     * Dependiendo del rol, decide si debe escuchar los levantamientos.
     */
    fun iniciarEscuchaDeRol() {
        repository.escucharRol { nuevoRol ->
            _rol.postValue(nuevoRol)
            // Si el rol es "responsable", se activa la escucha de reportes.
            if (nuevoRol == "responsable") {
                escucharLevantamiento()
            } else {
                // Si no es responsable, se asegura que la opción de levantamiento esté oculta.
                _hayLevantamiento.postValue(false)
            }
        }
    }

    /**
     * Inicia la escucha de reportes para el usuario actual.
     * Solo se debería llamar si el usuario es "responsable".
     */
    private fun escucharLevantamiento() {
        val userEmail = auth.currentUser?.email ?: ""
        repository.escucharLevantamiento(userEmail) { hayPendientes ->
            _hayLevantamiento.postValue(hayPendientes)
        }
    }

    /**
     * Se llama cuando el ViewModel está a punto de ser destruido.
     * Es el lugar ideal para limpiar recursos, como los listeners de Firebase.
     */
    override fun onCleared() {
        super.onCleared()
        repository.limpiarListeners()
    }
}