package com.example.reportes.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.reportes.data.model.Reporte
import com.example.reportes.data.repository.ReportesEstadoRepository
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.Locale

class ReportesEstadoViewModel : ViewModel() {

    private val repository = ReportesEstadoRepository()

    private val _rolUsuario = MutableLiveData<String>("usuario")
    val rolUsuario: LiveData<String> get() = _rolUsuario

    private val _reportes = MutableLiveData<List<Reporte>>(emptyList())
    val reportes: LiveData<List<Reporte>> get() = _reportes

    private val _pendientesCerradosIds = MutableLiveData<Set<String>>(emptySet())
    val pendientesCerradosIds: LiveData<Set<String>> get() = _pendientesCerradosIds

    fun cargarRolUsuario() {
        val email = FirebaseAuth.getInstance().currentUser?.email
        repository.obtenerRolUsuario(email) { rol ->
            _rolUsuario.postValue(rol)
        }
    }

    fun cargarReportes() {
        val rol = _rolUsuario.value ?: "usuario"
        val email = FirebaseAuth.getInstance().currentUser?.email
        repository.observarReportes(rol, email) { lista, pendientesIds ->
            _reportes.postValue(ordenarReportesPorFechaLimite(lista))
            _pendientesCerradosIds.postValue(pendientesIds)
        }
    }

    fun limpiarListeners() {
        repository.limpiarListeners()
    }

    fun filtrarReportes(estado: String): List<Reporte> {
        val listaReportes = _reportes.value ?: emptyList()
        val pendientesIds = _pendientesCerradosIds.value ?: emptySet()
        val rol = _rolUsuario.value ?: "usuario"

        val filtrados = when {
            estado == "Todos" || estado.isBlank() -> listaReportes
            rol != "admin" && estado == "Pendiente" -> listaReportes.filter { it.estado.equals("Pendiente", true) }
            rol != "admin" && estado == "Asignado" -> listaReportes.filter { it.estado.equals("Asignado", true) }
            estado == "Cerrado" -> listaReportes.filter {
                it.estado.equals("Cerrado", true) || it.estado.equals("Cerrado parcial", true)
            }
            else -> listaReportes.filter { it.estado.equals(estado, true) }
        }

        return filtrados.sortedWith { a, b ->
            // SOLO PARA ADMIN EN TAB ASIGNADO: los con botón "PENDIENTE" primero
            if (rol == "admin" && estado == "Asignado") {
                val aPendiente = pendientesIds.contains(a.id)
                val bPendiente = pendientesIds.contains(b.id)
                if (aPendiente && !bPendiente) return@sortedWith -1
                if (!aPendiente && bPendiente) return@sortedWith 1
            }
            val aCerradoParcial = a.estado.equals("Cerrado parcial", true)
            val bCerradoParcial = b.estado.equals("Cerrado parcial", true)
            val aCerrado = a.estado.equals("Cerrado", true)
            val bCerrado = b.estado.equals("Cerrado", true)
            if (estado == "Cerrado") {
                if (aCerradoParcial && !bCerradoParcial) return@sortedWith -1
                if (!aCerradoParcial && bCerradoParcial) return@sortedWith 1
            }
            if (estado == "Todos") {
                val aEsCerrado = aCerrado || aCerradoParcial
                val bEsCerrado = bCerrado || bCerradoParcial
                if (aEsCerrado && !bEsCerrado) return@sortedWith 1
                if (!aEsCerrado && bEsCerrado) return@sortedWith -1
            }
            if (a.fechaLimite.isEmpty()) return@sortedWith 1
            if (b.fechaLimite.isEmpty()) return@sortedWith -1
            try {
                val formatoFecha = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                val fechaA = formatoFecha.parse(a.fechaLimite)
                val fechaB = formatoFecha.parse(b.fechaLimite)
                fechaA.compareTo(fechaB)
            } catch (e: Exception) {
                a.fechaLimite.compareTo(b.fechaLimite)
            }
        }
    }

    private fun ordenarReportesPorFechaLimite(lista: List<Reporte>): List<Reporte> {
        return lista.sortedWith { a, b ->
            val aCerradoParcial = a.estado.equals("Cerrado parcial", true)
            val bCerradoParcial = b.estado.equals("Cerrado parcial", true)
            val aCerrado = a.estado.equals("Cerrado", true)
            val bCerrado = b.estado.equals("Cerrado", true)
            if (aCerrado && !bCerrado) return@sortedWith 1
            if (!aCerrado && bCerrado) return@sortedWith -1
            if (aCerradoParcial && !bCerradoParcial) {
                if (bCerrado) return@sortedWith -1
                return@sortedWith 1
            }
            if (!aCerradoParcial && bCerradoParcial) {
                if (aCerrado) return@sortedWith 1
                return@sortedWith -1
            }
            if (a.fechaLimite.isEmpty()) return@sortedWith 1
            if (b.fechaLimite.isEmpty()) return@sortedWith -1
            try {
                val formatoFecha = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                val fechaA = formatoFecha.parse(a.fechaLimite)
                val fechaB = formatoFecha.parse(b.fechaLimite)
                fechaA.compareTo(fechaB)
            } catch (e: Exception) {
                a.fechaLimite.compareTo(b.fechaLimite)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        limpiarListeners()
    }
}