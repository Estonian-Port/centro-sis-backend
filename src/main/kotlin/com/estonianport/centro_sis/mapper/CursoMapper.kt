package com.estonianport.centro_sis.mapper

import com.estonianport.centro_sis.dto.request.CursoAlquilerRequestDto
import com.estonianport.centro_sis.dto.request.CursoComisionRequestDto
import com.estonianport.centro_sis.dto.response.AlumnoResponseDto
import com.estonianport.centro_sis.dto.response.CursoAlumnoResponseDto
import com.estonianport.centro_sis.dto.response.CursoResponseDto
import com.estonianport.centro_sis.model.Curso
import com.estonianport.centro_sis.model.CursoAlquiler
import com.estonianport.centro_sis.model.CursoComision
import com.estonianport.centro_sis.model.Inscripcion
import com.estonianport.centro_sis.model.RolProfesor
import java.math.BigDecimal
import java.time.LocalDate
import java.time.format.DateTimeFormatter

object CursoMapper {

    fun buildCursoResponseDto(curso: Curso, alumnos: List<AlumnoResponseDto>): CursoResponseDto {
        val formatter: DateTimeFormatter? = DateTimeFormatter.ofPattern("dd-MM-yyyy")

        return CursoResponseDto(
            id = curso.id,
            nombre = curso.nombre,
            horarios = curso.horarios.map { HorarioMapper.buildHorarioResponseDto(it) }.toSet(),
            alumnosInscriptos = alumnos,
            fechaInicio = curso.fechaInicio.format(formatter),
            fechaFin = curso.fechaFin.format(formatter),
            estado = curso.estado.name,
            estadoAlta = curso.estadoAlta.name,
            profesores = curso.profesores.map { UsuarioMapper.buildUsuarioResponseDto(it.usuario) }.toSet(),
            tiposPago = curso.tiposPago.map { TipoPagoMapper.buildTipoPagoResponseDto(it) },
            inscripciones = curso.inscripciones.map { InscripcionMapper.buildInscripcionResponseDto(it) },
            recargoPorAtraso = curso.recargoAtraso
                .minus(BigDecimal.ONE)
                .multiply(BigDecimal(100))
                .toDouble()
        )
    }

    fun buildCursoAlumnoResponseDto(inscripcion: Inscripcion): CursoAlumnoResponseDto {
        return CursoAlumnoResponseDto(
            id = inscripcion.curso.id,
            nombre = inscripcion.curso.nombre,
            horarios = inscripcion.curso.horarios.map { HorarioMapper.buildHorarioResponseDto(it) }.toSet(),
            arancel = inscripcion.tipoPagoSeleccionado.monto.toDouble(),
            tipoPagoElegido = inscripcion.tipoPagoSeleccionado.tipo.name,
            profesores = inscripcion.curso.profesores.map { UsuarioMapper.buildNombreCompleto(it.usuario) }.toSet(),
            beneficio = inscripcion.beneficio,
            estadoPago = inscripcion.estadoPago.name
        )
    }

    fun buildCursoAlquiler(
        cursoDto: CursoAlquilerRequestDto,
        profesores: MutableSet<RolProfesor> = mutableSetOf()
    ): CursoAlquiler {
        return CursoAlquiler(
            id = cursoDto.id,
            nombre = cursoDto.nombre,
            profesores = profesores,
            precioAlquiler = cursoDto.montoAlquiler.toBigDecimal(),
            fechaInicio = LocalDate.parse(cursoDto.fechaInicio),
            fechaFin = LocalDate.parse(cursoDto.fechaFin),
        )
    }

    fun buildCursoComision(
        cursoDto: CursoComisionRequestDto,
        profesores: MutableSet<RolProfesor> = mutableSetOf()
    ): CursoComision {
        return CursoComision(
            id = cursoDto.id,
            nombre = cursoDto.nombre,
            profesores = profesores,
            horarios = cursoDto.horarios.map { HorarioMapper.buildHorario(it) }.toMutableList(),
            tiposPago = cursoDto.tipoPago.map { TipoPagoMapper.buildTipoPago(it) }.toMutableSet(),
            recargoAtraso = cursoDto.recargo
                ?.toBigDecimal()
                ?.divide(BigDecimal(100))
                ?.add(BigDecimal.ONE)
                ?: BigDecimal.ONE,
            porcentajeComision = cursoDto.comisionProfesor
                ?.toBigDecimal()
                ?.divide(BigDecimal(100))
                ?: BigDecimal("0.50"),
            fechaInicio = LocalDate.parse(cursoDto.fechaInicio),
            fechaFin = LocalDate.parse(cursoDto.fechaFin),
        )
    }
}