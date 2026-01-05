package com.estonianport.centro_sis.mapper

import com.estonianport.centro_sis.dto.request.CursoAlquilerRequestDto
import com.estonianport.centro_sis.dto.request.CursoComisionRequestDto
import com.estonianport.centro_sis.dto.response.CursoAlumnoResponseDto
import com.estonianport.centro_sis.dto.response.CursoInformacionResponseDto
import com.estonianport.centro_sis.dto.response.CursoResponseDto
import com.estonianport.centro_sis.model.Curso
import com.estonianport.centro_sis.model.CursoAlquiler
import com.estonianport.centro_sis.model.CursoComision
import com.estonianport.centro_sis.model.Inscripcion
import java.math.BigDecimal
import java.time.LocalDate
import java.time.format.DateTimeFormatter

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

    fun buildCursoInformacionResponseDto(curso: Curso, cantAlumnos: Int): CursoInformacionResponseDto {
        val formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy")

        return CursoInformacionResponseDto(
            id = curso.id,
            nombre = curso.nombre,
            horarios = curso.horarios.map { HorarioMapper.buildHorarioResponseDto(it) }.toSet(),
            alumnosInscriptos = cantAlumnos,
            fechaInicio = curso.fechaInicio.format(formatter),
            fechaFin = curso.fechaFin.format(formatter),
            estado = curso.estado.name,
            profesores = curso.profesores.map { UsuarioMapper.buildNombreCompleto(it.usuario) }.toSet()
        )
    }

    fun buildCursoAlquiler(cursoDto: CursoAlquilerRequestDto): CursoAlquiler {
        return CursoAlquiler(
            id = cursoDto.id,
            nombre = cursoDto.nombre,
            precioAlquiler = cursoDto.montoAlquiler.toBigDecimal(),
            fechaInicio = LocalDate.parse(cursoDto.fechaInicio),
            fechaFin = LocalDate.parse(cursoDto.fechaFin),
        )
    }

    fun buildCursoComision(cursoDto: CursoComisionRequestDto): CursoComision {
        return CursoComision(
            id = cursoDto.id,
            nombre = cursoDto.nombre,
            horarios = cursoDto.horarios.map { HorarioMapper.buildHorario(it) }.toMutableList(),
            tiposPago = cursoDto.tipoPago.map { TipoPagoMapper.buildTipoPago(it) }.toMutableSet(),
            recargoAtraso = cursoDto.recargo?.toBigDecimal() ?: BigDecimal.ONE,
            porcentajeComision = cursoDto.comisionProfesor?.toBigDecimal() ?: 0.5.toBigDecimal(),
            fechaInicio = LocalDate.parse(cursoDto.fechaInicio),
            fechaFin = LocalDate.parse(cursoDto.fechaFin),
        )
    }
}