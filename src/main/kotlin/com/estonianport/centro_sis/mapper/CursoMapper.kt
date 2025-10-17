package com.estonianport.centro_sis.mapper

import com.estonianport.centro_sis.dto.response.CursoResponseDto
import com.estonianport.centro_sis.model.Curso

object CursoMapper {

    fun buildCursoResponseDto(curso: Curso): CursoResponseDto {
        return CursoResponseDto(
            id = curso.id,
            nombre = curso.nombre,
            dias = curso.dias,
            horarios = curso.horarios,
            arancel = curso.arancel,
        )
    }

}