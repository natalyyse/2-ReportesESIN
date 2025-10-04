package com.example.reportes.data.model

data class ReporteCerrado(
    val reporteId: String = "",
    val comentario: String = "",
    val fechalevantamiento: String = "",
    val imagenUrl: String = "",
    val estadoCierre: String = "",
    val motivo: String? = null
)
