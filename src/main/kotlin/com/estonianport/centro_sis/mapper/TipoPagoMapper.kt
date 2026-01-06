package com.estonianport.centro_sis.mapper

import com.estonianport.centro_sis.dto.response.TipoPagoDto
import com.estonianport.centro_sis.model.TipoPago
import com.estonianport.centro_sis.model.enums.PagoType

object TipoPagoMapper {

    fun buildTipoPagoResponseDto(tipoPago: TipoPago): TipoPagoDto {
        return TipoPagoDto(
            tipo = tipoPago.tipo.name,
            monto = tipoPago.monto.toDouble()
        )
    }

    fun buildTipoPago(tipoPagoDto: TipoPagoDto): TipoPago {
        // Validar que el tipo no sea null o vacío
        require(tipoPagoDto.tipo.isNotBlank()) {
            "El tipo de pago no puede estar vacío"
        }

        // Convertir string a enum con manejo de errores
        val pagoType = try {
            PagoType.valueOf(tipoPagoDto.tipo.trim().uppercase())
        } catch (e: IllegalArgumentException) {
            throw IllegalArgumentException(
                "Tipo de pago inválido: '${tipoPagoDto.tipo}'. " +
                        "Valores permitidos: ${PagoType.entries.joinToString { it.name }}"
            )
        }

        return TipoPago(
            tipo = pagoType,
            monto = tipoPagoDto.monto.toBigDecimal()
        )
    }
}