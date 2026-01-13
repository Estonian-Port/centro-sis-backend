package com.estonianport.centro_sis.dto.response

import com.estonianport.centro_sis.model.Rol
import com.estonianport.centro_sis.model.enums.RolType

data class UsuarioResponseDto (
    val id: Long,
    val nombre: String,
    val apellido: String,
    val dni: String,
    val email: String,
    val celular: Long,
    val fechaNacimiento : String,
    val estado: String,
    val primerLogin: Boolean,
    val listaRol: MutableSet<RolType>,
    val ultimoIngreso: String
)

data class UsuarioDetailResponseDto (
    val id: Long,
    val nombre: String,
    val apellido: String,
    val dni: String,
    val email: String,
    val celular: Long,
    val fechaNacimiento : String,
    val estado: String,
    val primerLogin: Boolean,
    val listaRol: MutableSet<RolType>,
    val cursosInscriptos: List<CursoAlumnoResponseDto>,
    val cursosDictados: List<CursoResponseDto>,
    //val pagos: List<PagoResponseDto>?
)

