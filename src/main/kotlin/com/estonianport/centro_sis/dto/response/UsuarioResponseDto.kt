package com.estonianport.centro_sis.dto.response

import com.estonianport.centro_sis.model.Rol
import com.estonianport.centro_sis.model.enums.RolType

data class UsuarioResponseDto (
    val id: Long,
    val nombre: String,
    val apellido: String,
    val email: String,
    val celular: Long,
    val primerLogin: Boolean,
    val listaRol: MutableSet<RolType>
)
