package com.estonianport.centro_sis.dto.response

data class AlumnoResponseDto (
    val id : Long,
    val nombre : String,
    val apellido : String,
    val dni : String,
    val email : String,
    val celular : String,
)
