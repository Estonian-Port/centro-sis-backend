package com.estonianport.centro_sis.dto.response

import com.estonianport.centro_sis.model.enums.EstadoPagoType

data class AlumnoResponseDto (
    val id : Long,
    val nombre : String,
    val apellido : String,
    val dni : String,
    val email : String,
    val celular : String,
    val estadoPago : String,
    val tipoPagoElegido : String,
    val asistencias : Int,
    val beneficio : Double?,
    val puntos : Int,
)
