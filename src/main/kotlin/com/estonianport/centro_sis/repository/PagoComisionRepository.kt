package com.estonianport.centro_sis.repository

import com.estonianport.centro_sis.model.PagoComision
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
interface PagoComisionRepository : JpaRepository<PagoComision, Long> {

    // Buscar por curso
    fun findByCursoIdAndFechaBajaIsNull(cursoId: Long): List<PagoComision>

    // Buscar por profesor
    fun findByProfesorIdAndFechaBajaIsNull(profesorId: Long): List<PagoComision>

    // Buscar por periodo (mes/año)
    fun findByMesPagoAndAnioPagoAndFechaBajaIsNull(
        mes: Int,
        anio: Int
    ): List<PagoComision>

    // Buscar por rango de fechas
    @Query("""
        SELECT pc FROM PagoComision pc
        WHERE pc.fecha BETWEEN :desde AND :hasta
        AND pc.fechaBaja IS NULL
    """)
    fun findByFechaBetweenAndFechaBajaIsNull(
        @Param("desde") desde: LocalDate,
        @Param("hasta") hasta: LocalDate
    ): List<PagoComision>
}