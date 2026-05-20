package com.estonianport.centro_sis.repository

import com.estonianport.centro_sis.model.PagoComision
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.time.LocalDateTime

@Repository
interface PagoComisionRepository : JpaRepository<PagoComision, Long> {

    // Buscar por rango de fechas
    @Query("""
        SELECT pc FROM PagoComision pc
        WHERE pc.fecha BETWEEN :desde AND :hasta
        AND pc.fechaBaja IS NULL
    """)
    fun findByFechaBetweenAndFechaBajaIsNull(
        @Param("desde") desde: LocalDateTime,
        @Param("hasta") hasta: LocalDateTime
    ): List<PagoComision>
}