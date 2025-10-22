package com.estonianport.centro_sis.dto.response

import java.time.LocalDate

data class PagoResponseDto(
    val id: Long,
    val alumnoId: Long,
    val monto: Double,
    val fecha: LocalDate,
    val retraso: Boolean,
    val fechaBaja: LocalDate?
)
