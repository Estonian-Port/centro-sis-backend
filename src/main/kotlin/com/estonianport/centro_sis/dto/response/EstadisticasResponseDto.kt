package com.estonianport.centro_sis.dto.response

data class EstadisticasResponseDto (
    val totalUsuarios: Int,
    val usuariosActivos: Int,
    val usuariosInactivos: Int,
    val usuariosPendientes: Int,
)