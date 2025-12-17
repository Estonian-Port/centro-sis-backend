// mapper/PagoMapper.kt
package com.estonianport.centro_sis.mapper

import com.estonianport.centro_sis.dto.response.PagoResponseDto
import com.estonianport.centro_sis.model.Pago

object PagoMapper {

    fun buildPagoResponseDto(pago: Pago): PagoResponseDto {
        return PagoResponseDto(
            id = pago.id,
            alumnoId = pago.inscripcion.alumno.id,
            monto = pago.monto,
            fecha = pago.fecha,
            retraso = pago.retraso,
            fechaBaja = pago.fechaBaja
        )
    }
}
