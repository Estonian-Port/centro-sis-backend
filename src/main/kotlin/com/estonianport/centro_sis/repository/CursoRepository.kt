package com.estonianport.centro_sis.repository

import com.estonianport.centro_sis.model.Curso
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface CursoRepository : CrudRepository<Curso, Long> {
/*
    @Query(
        """
    SELECT COUNT(DISTINCT u)
    FROM Curso c
    WHERE u.ultimoIngreso IS NOT NULL
    AND u.fechaBaja IS NULL
"""
    )
    fun getCursosByUsuario(): Int*/
}