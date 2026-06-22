package com.estonianport.centro_sis.mapper

import com.estonianport.centro_sis.dto.response.PagoRealizadoDto
import com.estonianport.centro_sis.dto.response.PagoResponseDto
import com.estonianport.centro_sis.dto.response.TipoPagoDto
import com.estonianport.centro_sis.model.Pago
import com.estonianport.centro_sis.model.PagoCurso

object PagoMapper {

    fun buildPagoResponseDto(pago: Pago): PagoResponseDto {
        return PagoResponseDto(
            id = pago.id,
            monto = pago.monto.toDouble(),
            fecha = pago.fecha.toLocalDate(),
            fechaBaja = pago.fechaBaja
        )
    }

    fun PagoCurso.toPagoRealizadoDto(): PagoRealizadoDto {
        return PagoRealizadoDto(
            id = id,
            tipoPago = TipoPagoDto(
                tipo = inscripcion.tipoPagoSeleccionado.tipo.toString(),
                monto = monto.toDouble(),
                cuotas = cuotasParaLiquidacion
            ),
            fecha = fecha.toLocalDate(),
            retraso = conRecargo
        )
    }
}
