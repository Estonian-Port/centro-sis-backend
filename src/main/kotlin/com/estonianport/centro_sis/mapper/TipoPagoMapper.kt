package com.estonianport.centro_sis.mapper

import com.estonianport.centro_sis.dto.response.TipoPagoDto
import com.estonianport.centro_sis.model.TipoPago

object TipoPagoMapper {

    fun buildTipoPagoResponseDto(tipoPago: TipoPago) : TipoPagoDto {
        return TipoPagoDto(
            tipo = tipoPago.tipoPago.name,
            monto = tipoPago.monto.toDouble()
        )
    }

    fun buildTipoPago(tipoPagoDto: TipoPagoDto) : TipoPago {
        return TipoPago(
            tipoPago = enumValueOf(tipoPagoDto.tipo),
            monto = tipoPagoDto.monto.toBigDecimal()
        )
    }
}