package com.estonianport.centro_sis.dto.response

data class InscripcionResponseDto (
    val id: Long,
    val nombreAlumno: String,
    val nombreCurso: String,
    val tipoPagoSeleccionado: String,
    val beneficio: Int,
    val fechaInscripcion: String,
    val estadoPago: String,
)