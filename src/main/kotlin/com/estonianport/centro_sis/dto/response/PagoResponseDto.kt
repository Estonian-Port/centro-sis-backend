package com.estonianport.centro_sis.dto.response

import java.io.Serializable
import java.time.LocalDate

data class PagoResponseDto(
    val id: Long,
    val monto: Double,
    val fecha: LocalDate,
    val fechaBaja: LocalDate?
)

data class PagoRealizadoDto(
    val id: Long,
    val tipoPago: TipoPagoDto,
    val fecha: LocalDate,
    val retraso: Boolean
): Serializable


