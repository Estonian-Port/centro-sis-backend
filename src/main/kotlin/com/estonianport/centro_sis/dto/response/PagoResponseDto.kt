package com.estonianport.centro_sis.dto.response

import java.time.LocalDate
import java.time.LocalDateTime

data class PagoResponseDto(
    val id: Long,
    val monto: Double,
    val fecha: LocalDate,
    val fechaBaja: LocalDate?
)
