package com.estonianport.centro_sis.mapper

import com.estonianport.centro_sis.dto.request.CursoRequestDto
import com.estonianport.centro_sis.dto.response.CursoResponseDto
import com.estonianport.centro_sis.model.Curso
import com.estonianport.centro_sis.model.Usuario
import com.estonianport.centro_sis.model.enums.PagoType

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

    fun buildCurso(cursoDto: CursoRequestDto): Curso {
        return Curso(
            id = cursoDto.id,
            nombre = cursoDto.nombre,
            horarios = cursoDto.horarios.map { HorarioMapper.buildHorario(it) }.toMutableList(),
            arancel = cursoDto.arancel,
            tiposPago = cursoDto.tipoPago.map { PagoType.valueOf(it) }.toMutableSet()
        )
    }

}