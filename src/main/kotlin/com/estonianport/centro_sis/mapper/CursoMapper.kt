package com.estonianport.centro_sis.mapper

import com.estonianport.centro_sis.dto.response.CursoAlumnoResponseDto
import com.estonianport.centro_sis.dto.response.CursoProfesorResponseDto
import com.estonianport.centro_sis.dto.response.CursoResponseDto
import com.estonianport.centro_sis.model.Curso
import com.estonianport.centro_sis.model.Inscripcion

object CursoMapper {

    fun buildCursoResponseDto(curso: Curso): CursoResponseDto {
        return CursoResponseDto(
            id = curso.id,
            nombre = curso.nombre,
            horarios = curso.horarios.map { HorarioMapper.buildHorarioResponseDto(it) }.toSet(),
            tiposPago = curso.tiposPago.map { TipoPagoMapper.buildTipoPagoResponseDto(it) }.toSet(),
            profesores = curso.profesores.map { UsuarioMapper.buildNombreCompleto(it.usuario) }.toSet()
        )
    }

    fun buildCursoAlumnoResponseDto(inscripcion: Inscripcion): CursoAlumnoResponseDto {
        return CursoAlumnoResponseDto(
            id = inscripcion.curso.id,
            nombre = inscripcion.curso.nombre,
            horarios = inscripcion.curso.horarios.map { HorarioMapper.buildHorarioResponseDto(it) }.toSet(),
            arancel = inscripcion.tipoPagoSeleccionado.monto.toDouble(),
            tipoPagoElegido = inscripcion.tipoPagoSeleccionado.tipoPago.name,
            profesores = inscripcion.curso.profesores.map { UsuarioMapper.buildNombreCompleto(it.usuario) }.toSet(),
            beneficio = inscripcion.beneficio,
            estadoPago = inscripcion.estadoPago.name
        )
    }

    fun buildCursoProfesorResponseDto(curso: Curso, cantAlumnos: Int): CursoProfesorResponseDto {
        return CursoProfesorResponseDto(
            id = curso.id,
            nombre = curso.nombre,
            horarios = curso.horarios.map { HorarioMapper.buildHorarioResponseDto(it) }.toSet(),
            alumnosInscriptos = cantAlumnos,
            fechaIncio = curso.fechaInicio.toString(),
            fechaFin = curso.fechaFin.toString(),
            estado = curso.estado.name,
        )
    }
    /*
        fun buildCurso(cursoDto: CursoRequestDto): Curso {
            return Curso(
                id = cursoDto.id,
                nombre = cursoDto.nombre,
                horarios = cursoDto.horarios.map { HorarioMapper.buildHorario(it) }.toMutableList(),
                arancel = cursoDto.arancel,
                tiposPago = cursoDto.tipoPago.map { PagoType.valueOf(it) }.toMutableSet(),
                fechaInicio = LocalDate.parse(cursoDto.fechaInicio),
                fechaFin = LocalDate.parse(cursoDto.fechaFin),
            )
        }
    */
}