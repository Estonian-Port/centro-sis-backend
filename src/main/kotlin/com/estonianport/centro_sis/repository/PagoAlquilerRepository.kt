package com.estonianport.centro_sis.repository

import com.estonianport.centro_sis.model.PagoAlquiler
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
interface PagoAlquilerRepository : JpaRepository<PagoAlquiler, Long> {

    // Buscar por curso
    fun findByCursoIdAndFechaBajaIsNull(cursoId: Long): List<PagoAlquiler>

    // Buscar por profesor
    fun findByProfesorIdAndFechaBajaIsNull(profesorId: Long): List<PagoAlquiler>

    // Buscar por periodo (mes/año)
    fun findByMesPagoAndAnioPagoAndFechaBajaIsNull(
        mes: Int,
        anio: Int
    ): List<PagoAlquiler>

    // Buscar por rango de fechas
    @Query("""
        SELECT pa FROM PagoAlquiler pa
        WHERE pa.fecha BETWEEN :desde AND :hasta
        AND pa.fechaBaja IS NULL
    """)
    fun findByFechaBetweenAndFechaBajaIsNull(
        @Param("desde") desde: LocalDate,
        @Param("hasta") hasta: LocalDate
    ): List<PagoAlquiler>
}