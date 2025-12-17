package com.estonianport.centro_sis.dto.response

data class EstadisticasResponseDto (
    val alumnosActivos: Long,
    val cursos: Long,
    val profesores: Long,
    val ingresosMensuales: Double
)