package com.estonianport.centro_sis.dto.response

data class CustomResponse(
    val message: String,
    val data: Any? = null,
)