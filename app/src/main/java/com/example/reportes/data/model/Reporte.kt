package com.example.reportes.data.model
import java.io.Serializable
data class Reporte(
    val id: String = "",
    val tipo: String = "",
    val nivelRiesgo: String = "",
    val descripcion: String = "",
    val lugar: String = "",
    val fechacreacion: String = "",  
    val reportante: String = "",    
    val imagenUrl: String = "",
    val estado: String = "",
    val fechaLimite: String = "",
    val responsable: String = "",
    val fechaAsignacion: String = ""
) : Serializable