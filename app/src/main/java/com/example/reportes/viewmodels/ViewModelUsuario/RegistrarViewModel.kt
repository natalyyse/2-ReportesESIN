package com.example.reportes.viewmodels.ViewModelUsuario

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.reportes.data.repository.usuario.RegistrarRepository

// ViewModel que gestiona la lógica de la pantalla de registro.
class RegistrarViewModel(
    // Inyección del repositorio para la comunicación con la capa de datos.
    private val repository: RegistrarRepository = RegistrarRepository()
) : ViewModel() {

    // Define los posibles estados de la UI para la pantalla de registro.
    sealed class UiState {
        object Idle : UiState() // Estado inicial
        object Loading : UiState() // Cargando
        object Success : UiState() // Operación exitosa
        data class Error(val message: String) : UiState() // Ocurrió un error
    }

    // LiveData privado para el estado de la UI, solo modificable dentro del ViewModel.
    private val _uiState = MutableLiveData<UiState>(UiState.Idle)
    // LiveData público y de solo lectura para ser observado por la UI.
    val uiState: LiveData<UiState> = _uiState

    /**
     * Inicia el proceso de registro de usuario with validaciones básicas.
     * @param email Correo electrónico del usuario.
     * @param password Contraseña del usuario.
     * @param confirmPassword Confirmación de la contraseña.
     */
    fun registrarUsuario(email: String, password: String, confirmPassword: String) {
        // Validación de campos vacíos
        if (email.isBlank() || password.isBlank() || confirmPassword.isBlank()) {
            _uiState.value = UiState.Error("Todos los campos son obligatorios.")
            return
        }
        // Validación de formato de email.
        if (!email.contains("@")) {
            _uiState.value = UiState.Error("Correo inválido")
            return
        }
        // Validación de coincidencia de contraseñas.
        if (password != confirmPassword) {
            _uiState.value = UiState.Error("Las contraseñas no coinciden.")
            return
        }

        // Cambia el estado a Loading antes de llamar al repositorio.
        _uiState.value = UiState.Loading

        // Llama al método del repositorio para registrar al usuario.
        repository.registrarUsuario(email, password) { result ->
            when (result) {
                is RegistrarRepository.Result.Success -> {
                    // Publica el estado de éxito si el registro fue correcto.
                    _uiState.postValue(UiState.Success)
                }
                is RegistrarRepository.Result.Error -> {
                    // Publica el estado de error con el mensaje correspondiente.
                    _uiState.postValue(UiState.Error(result.message))
                }
            }
        }
    }
}