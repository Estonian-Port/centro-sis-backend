package com.estonianport.centro_sis.dto

import com.estonianport.centro_sis.model.enums.TipoAcceso
import java.time.LocalDateTime

/**
 * DTO para mostrar un acceso en el frontend
 */
data class AccesoDTO(
    val id: Long,
    val usuarioId: Long,
    val usuarioNombre: String,
    val usuarioApellido: String,
    val usuarioDni: String,
    val fechaHora: LocalDateTime,
    val tipoAcceso: TipoAcceso
)

/**
 * DTO para registrar un acceso manualmente
 */
data class RegistrarAccesoDTO(
    val usuarioId: Long
)