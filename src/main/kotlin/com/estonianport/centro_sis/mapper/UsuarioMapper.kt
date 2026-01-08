package com.estonianport.centro_sis.mapper

import com.estonianport.centro_sis.dto.ProfesorListaResponseDto
import com.estonianport.centro_sis.dto.response.UsuarioPendienteResponseDto
import com.estonianport.centro_sis.dto.response.UsuarioRegistradoResponseDto
import com.estonianport.centro_sis.dto.response.UsuarioResponseDto
import com.estonianport.centro_sis.dto.request.UsuarioAltaRequestDto
import com.estonianport.centro_sis.dto.request.UsuarioRequestDto
import com.estonianport.centro_sis.dto.response.AlumnoResponseDto
import com.estonianport.centro_sis.dto.response.CursoAlumnoResponseDto
import com.estonianport.centro_sis.dto.response.CursoResponseDto
import com.estonianport.centro_sis.dto.response.UsuarioDetailResponseDto
import com.estonianport.centro_sis.model.Inscripcion
import com.estonianport.centro_sis.model.RolAlumno
import com.estonianport.centro_sis.model.Usuario
import com.estonianport.centro_sis.model.enums.EstadoType
import com.estonianport.centro_sis.model.enums.RolType
import java.time.format.DateTimeFormatter

object UsuarioMapper {

    fun buildUsuarioResponseDto(usuario: Usuario): UsuarioResponseDto {
        return UsuarioResponseDto(
            id = usuario.id,
            nombre = usuario.nombre,
            apellido = usuario.apellido,
            dni = usuario.dni,
            email = usuario.email,
            celular = usuario.celular,
            estado = usuario.estado.name,
            primerLogin = usuario.estado == EstadoType.PENDIENTE,
            listaRol = usuario.getRolTypes().toMutableSet()
        )
    }

    fun buildNombreCompleto(usuario: Usuario): String {
        return "${usuario.nombre} ${usuario.apellido}"
    }

    fun buildUsuario(usuarioDto: UsuarioRequestDto): Usuario {
        return Usuario(
            id = usuarioDto.id,
            nombre = usuarioDto.nombre,
            apellido = usuarioDto.apellido,
            dni = usuarioDto.dni,
            celular = usuarioDto.celular,
            email = usuarioDto.email
        )
    }

    fun buildAltaUsuario(usuarioDto: UsuarioAltaRequestDto): Usuario {
        return Usuario(
            id = 0,
            nombre = "",
            apellido = "",
            dni = "",
            celular = 0,
            email = usuarioDto.email
        )
    }

    fun buildProfesoresListaResponseDto(usuario: Usuario): ProfesorListaResponseDto {
        return ProfesorListaResponseDto(
            id = usuario.id,
            nombre = usuario.nombre,
            apellido = usuario.apellido,
        )
    }

    fun buildUsuarioDetailDto(
        usuario: Usuario,
        cursosInscriptos: List<CursoAlumnoResponseDto>?,
        cursosDictados: List<CursoResponseDto>?
    ): UsuarioDetailResponseDto {
        return UsuarioDetailResponseDto(
            id = usuario.id,
            nombre = usuario.nombre,
            apellido = usuario.apellido,
            dni = usuario.dni,
            email = usuario.email,
            celular = usuario.celular,
            estado = usuario.estado.name,
            primerLogin = usuario.estado == EstadoType.PENDIENTE,
            listaRol = usuario.getRolTypes().toMutableSet(),
            cursosInscriptos = cursosInscriptos.orEmpty(),
            cursosDictados = cursosDictados.orEmpty(),
        )
    }

    fun buildAlumno(inscripcion: Inscripcion): AlumnoResponseDto {
        return AlumnoResponseDto(
            id = inscripcion.alumno.usuario.id,
            nombre = inscripcion.alumno.usuario.nombre,
            apellido = inscripcion.alumno.usuario.apellido,
            dni = inscripcion.alumno.usuario.dni,
            email = inscripcion.alumno.usuario.email,
            celular = inscripcion.alumno.usuario.celular.toString(),
            estadoPago = inscripcion.estadoPago.name,
            tipoPagoElegido = inscripcion.tipoPagoSeleccionado.tipo.name,
            asistencias = 0, // Este campo debe ser calculado aparte
            beneficio = inscripcion.beneficio.toDouble(),
            puntos = inscripcion.puntos
        )
    }

}