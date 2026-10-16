package com.estonianport.centro_sis.repository

import com.estonianport.centro_sis.model.PagoCurso
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.time.LocalDateTime

@Repository
interface PagoCursoRepository : JpaRepository<PagoCurso, Long> {

    // Buscar por rango de fechas
    @Query("""
        SELECT pc FROM PagoCurso pc
        WHERE pc.fecha BETWEEN :desde AND :hasta
        AND pc.fechaBaja IS NULL
    """)
    fun findByFechaBetweenAndFechaBajaIsNull(
        @Param("desde") desde: LocalDateTime,
        @Param("hasta") hasta: LocalDateTime
    ): List<PagoCurso>

    @Query("""
        SELECT pc FROM PagoCurso pc
        JOIN FETCH pc.inscripcion i
        WHERE i.curso.id = :cursoId
        AND pc.fechaBaja IS NULL
    """)
    fun findActivosByCursoId(@Param("cursoId") cursoId: Long): List<PagoCurso>
}
