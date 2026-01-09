package com.estonianport.centro_sis.dto.response

data class InscripcionResponseDto (
    val id: Long,
    val alumno: AlumnoResponseDto,
    val tipoPagoElegido: TipoPagoDto,
    val fechaInscripcion: String,
    val pagosRealizados: List<PagoResponseDto>,
    val estadoPago: String,
    val beneficio: Int,
    val puntos: Int,
    val porcentajeAsistencia: Double,
    val estado : String
)
