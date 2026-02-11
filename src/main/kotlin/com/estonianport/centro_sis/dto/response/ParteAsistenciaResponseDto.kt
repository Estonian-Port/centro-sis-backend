package com.estonianport.centro_sis.dto.response

data class ParteAsistenciaResponseDto(
    val id: Long,
    val fecha: String,
    val totalAlumnos: Int,
    val totalPresentes: Int,
    val totalAusentes: Int,
    val presentes: List<UsuarioSimpleResponseDto>,
    val ausentes: List<UsuarioSimpleResponseDto>,
    val porcentajeAsistencia: Double,
    val tomadoPor: UsuarioSimpleResponseDto
)