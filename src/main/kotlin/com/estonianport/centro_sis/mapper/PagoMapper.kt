package com.estonianport.centro_sis.mapper

import com.estonianport.centro_sis.dto.response.PagoResponseDto
import com.estonianport.centro_sis.model.Pago

object PagoMapper {

    fun buildPagoResponseDto(pago: Pago): PagoResponseDto {
        return PagoResponseDto(
            id = pago.id,
            monto = pago.monto.toDouble(),
            fecha = pago.fecha.toLocalDate(),
            fechaBaja = pago.fechaBaja
        )
    }
}
