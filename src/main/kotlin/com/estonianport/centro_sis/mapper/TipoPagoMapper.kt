package com.estonianport.centro_sis.mapper

import com.estonianport.centro_sis.dto.response.TipoPagoResponseDto
import com.estonianport.centro_sis.model.TipoPago

object TipoPagoMapper {

    fun buildTipoPagoResponseDto(tipoPago: TipoPago) : TipoPagoResponseDto {
        return TipoPagoResponseDto(
            tipo = tipoPago.tipoPago.name,
            monto = tipoPago.monto.toDouble()
        )
    }
}