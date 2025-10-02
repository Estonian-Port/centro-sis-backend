package com.estonianport.centro_sis.mapper

import com.estonianport.centro_sis.dto.response.UsuarioPendienteResponseDto
import com.estonianport.centro_sis.dto.response.UsuarioRegistradoResponseDto
import com.estonianport.centro_sis.dto.response.UsuarioResponseDto
import com.estonianport.unique.dto.request.UsuarioAltaRequestDto
import com.estonianport.unique.dto.request.UsuarioRequestDto
import com.estonianport.unique.model.Usuario
import com.estonianport.unique.model.enums.EstadoType
import java.time.format.DateTimeFormatter

object UsuarioMapper {

    fun buildUsuarioResponseDto(usuario: Usuario): UsuarioResponseDto {
        return UsuarioResponseDto(
            id = usuario.id,
            nombre = usuario.nombre,
            apellido = usuario.apellido,
            email = usuario.email,
            celular = usuario.celular,
            isAdmin = usuario.esAdministrador,
            primerLogin = usuario.estado == EstadoType.PENDIENTE
        )
    }

    fun buildUsuarioRegistradoResponseDto(usuario: Usuario): UsuarioRegistradoResponseDto {
        return UsuarioRegistradoResponseDto(
            id = usuario.id,
            nombre = usuario.nombre,
            apellido = usuario.apellido,
            celular = usuario.celular,
            email = usuario.email,
            estado = usuario.estado.name,
        )
    }

    fun buildUsuarioPendienteResponseDto(usuario: Usuario): UsuarioPendienteResponseDto {
        return UsuarioPendienteResponseDto(
            id = usuario.id,
            email = usuario.email,
            fechaAlta = usuario.fechaAlta.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
        )
    }


    fun buildUsuario(usuarioDto: UsuarioRequestDto) : Usuario {
        return Usuario (
            id = usuarioDto.id,
            nombre = usuarioDto.nombre,
            apellido = usuarioDto.apellido,
            celular = usuarioDto.celular,
            email = usuarioDto.email
        )
    }

    fun buildAltaUsuario(usuarioDto: UsuarioAltaRequestDto) : Usuario {
        return Usuario (
            id = 0,
            nombre = "",
            apellido = "",
            celular = 0,
            email = usuarioDto.email
        )
    }

}