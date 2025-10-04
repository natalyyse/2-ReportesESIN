package com.example.reportes.viewmodels.ViewModelUsuario

import android.app.Application
import androidx.lifecycle.*
import com.example.reportes.data.model.User
import com.example.reportes.data.repository.usuario.MainRepository
import com.google.android.gms.auth.api.signin.GoogleSignInAccount

class MainViewModel(application: Application) : AndroidViewModel(application) {

    // --- Estados de Autenticación ---
    sealed class AuthResult {
        object Success : AuthResult()
        data class Error(val message: String) : AuthResult()
        object Loading : AuthResult()
    }

    // --- LiveData para comunicar el resultado a la UI ---
    private val _authResult = MutableLiveData<AuthResult>()
    val authResult: LiveData<AuthResult> = _authResult

    private val repository = MainRepository(application.applicationContext)

    // --- Lógica de Negocio ---

    /** Inicia sesión con Email y Password */
    fun signInWithEmail(email: String, password: String) {
        _authResult.value = AuthResult.Loading
        repository.signInWithEmail(email, password, object : MainRepository.AuthCallback {
            override fun onSuccess(user: User) {
                _authResult.postValue(AuthResult.Success)
            }

            override fun onError(message: String) {
                _authResult.postValue(AuthResult.Error(message))
            }
        })
    }

    /** Inicia sesión con una cuenta de Google */
    fun signInWithGoogle(account: GoogleSignInAccount) {
        _authResult.value = AuthResult.Loading
        repository.signInWithGoogle(account, object : MainRepository.AuthCallback {
            override fun onSuccess(user: User) {
                _authResult.postValue(AuthResult.Success)
            }

            override fun onError(message: String) {
                _authResult.postValue(AuthResult.Error(message))
            }
        })
    }
}