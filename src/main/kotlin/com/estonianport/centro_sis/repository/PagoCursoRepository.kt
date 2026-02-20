package com.estonianport.centro_sis.repository

import com.estonianport.centro_sis.model.PagoCurso
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
interface PagoCursoRepository : JpaRepository<PagoCurso, Long> {

    // Buscar por inscripción
    fun findByInscripcionId(inscripcionId: Long): List<PagoCurso>

    // Buscar activos por inscripción
    fun findByInscripcionIdAndFechaBajaIsNull(inscripcionId: Long): List<PagoCurso>

    // Buscar por rango de fechas
    @Query("""
        SELECT pc FROM PagoCurso pc
        WHERE pc.fecha BETWEEN :desde AND :hasta
        AND pc.fechaBaja IS NULL
    """)
    fun findByFechaBetweenAndFechaBajaIsNull(
        @Param("desde") desde: LocalDate,
        @Param("hasta") hasta: LocalDate
    ): List<PagoCurso>
}