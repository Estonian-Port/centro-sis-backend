package com.estonianport.centro_sis.mapper

import com.estonianport.centro_sis.dto.request.CursoRequestDto
import com.estonianport.centro_sis.dto.response.CursoAlumnoResponseDto
import com.estonianport.centro_sis.dto.response.CursoProfesorResponseDto
import com.estonianport.centro_sis.dto.response.CursoResponseDto
import com.estonianport.centro_sis.model.Curso
import com.estonianport.centro_sis.model.Usuario
import com.estonianport.centro_sis.model.enums.BeneficioType
import com.estonianport.centro_sis.model.enums.EstadoPagoType
import com.estonianport.centro_sis.model.enums.PagoType
import java.time.LocalDate

object CursoMapper {

    fun buildCursoResponseDto(curso: Curso, profesores : List<Usuario>): CursoResponseDto {
        return CursoResponseDto(
            id = curso.id,
            nombre = curso.nombre,
            horarios = curso.horarios.map{ HorarioMapper.buildHorarioResponseDto(it) }.toSet(),
            arancel = curso.arancel,
            tiposPago = curso.tiposPago.map { it.name }.toSet(),
            profesores = profesores.map { UsuarioMapper.buildNombreCompleto(it) }.toSet()
        )
    }

    fun buildCursoAlumnoResponseDto(curso: Curso, profesores : List<Usuario>, beneficiosAlumno : MutableSet<BeneficioType>, estadoPago : EstadoPagoType ) : CursoAlumnoResponseDto {
        return CursoAlumnoResponseDto(
            id = curso.id,
            nombre = curso.nombre,
            horarios = curso.horarios.map{ HorarioMapper.buildHorarioResponseDto(it) }.toSet(),
            arancel = curso.arancel,
            tiposPago = curso.tiposPago.map { it.name }.toSet(),
            profesores = profesores.map { UsuarioMapper.buildNombreCompleto(it) }.toSet(),
            beneficios = beneficiosAlumno.map { it.name }.toSet(),
            estadoPago = estadoPago.name
        )
    }

    fun buildCursoProfesorResponseDto(curso: Curso, cantAlumnos : Int): CursoProfesorResponseDto {
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

}