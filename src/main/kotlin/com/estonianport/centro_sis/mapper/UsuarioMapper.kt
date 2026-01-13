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
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

object UsuarioMapper {
    val formatter: DateTimeFormatter? = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    val formatterTime: DateTimeFormatter? = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")

    fun buildUsuarioResponseDto(usuario: Usuario): UsuarioResponseDto {
        return UsuarioResponseDto(
            id = usuario.id,
            nombre = usuario.nombre,
            apellido = usuario.apellido,
            dni = usuario.dni,
            email = usuario.email,
            celular = usuario.celular,
            fechaNacimiento = usuario.fechaNacimiento.format(formatter),
            estado = usuario.estado.name,
            primerLogin = usuario.esPrimerLogin(),
            listaRol = usuario.getRolTypes().toMutableSet(),
            ultimoIngreso = usuario.ultimoIngreso?.format(formatterTime) ?: "",
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
            email = usuarioDto.email,
            celular = usuarioDto.celular,
            fechaNacimiento = LocalDate.parse(usuarioDto.fechaDeNacimiento),
        )
    }

    fun buildAltaUsuario(usuarioDto: UsuarioAltaRequestDto): Usuario {
        return Usuario(
            id = 0,
            nombre = "",
            apellido = "",
            dni = "",
            celular = 0,
            email = usuarioDto.email,
            fechaNacimiento = LocalDate.now(),
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
            fechaNacimiento = usuario.fechaNacimiento.format(formatter),
            estado = usuario.estado.name,
            primerLogin = usuario.esPrimerLogin(),
            listaRol = usuario.getRolTypes().toMutableSet(),
            cursosInscriptos = cursosInscriptos.orEmpty(),
            cursosDictados = cursosDictados.orEmpty(),
        )
    }

    fun buildAlumno(alumno: Usuario): AlumnoResponseDto {
        return AlumnoResponseDto(
            id = alumno.id,
            nombre = alumno.nombre,
            apellido = alumno.apellido,
            dni = alumno.dni,
            email = alumno.email,
            celular = alumno.celular.toString(),
            fechaNacimiento = alumno.fechaNacimiento.format(formatter),
        )
    }

}