package com.estonianport.centro_sis.dto.response

import com.estonianport.centro_sis.dto.request.AdultoResponsableDto
import com.estonianport.centro_sis.model.enums.RolType
import java.io.Serializable

data class UsuarioResponseDto(
    val id: Long,
    val nombre: String,
    val apellido: String,
    val dni: String,
    val email: String,
    val celular: Long,
    val fechaNacimiento: String,
    val estado: String,
    val primerLogin: Boolean,
    val listaRol: MutableSet<RolType>,
    val ultimoIngreso: String,
    val adultoResponsable: AdultoResponsableDto? = null
): Serializable

data class UsuarioDetailResponseDto(
    val id: Long,
    val nombre: String,
    val apellido: String,
    val dni: String,
    val email: String,
    val celular: Long,
    val fechaNacimiento: String,
    val estado: String,
    val primerLogin: Boolean,
    val listaRol: MutableSet<RolType>,
    val ultimoIngreso: String,
    val adultoResponsable: AdultoResponsableDto? = null,
    val cursosInscriptos: List<CursoAlumnoResponseDto>,
    val cursosDictados: List<CursoResponseDto>,
): Serializable

data class UsuarioSimpleResponseDto(
    val id: Long,
    val nombre: String,
    val apellido: String,
): Serializable

