package com.estonianport.centro_sis.repository

import com.estonianport.centro_sis.model.PagoMatricula
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface PagoMatriculaRepository : JpaRepository<PagoMatricula, Long> {

    // ¿El alumno ya pagó la matrícula de ese año? (solo pagos activos)
    fun existsByAlumnoIdAndAnioAndFechaBajaIsNull(alumnoId: Long, anio: Int): Boolean

    // Matrícula activa del alumno para ese año (si existe)
    fun findFirstByAlumnoIdAndAnioAndFechaBajaIsNull(alumnoId: Long, anio: Int): PagoMatricula?

    // Para el reporte financiero (matrículas son ingreso del instituto)
    @Query("""
        SELECT pm FROM PagoMatricula pm
        WHERE pm.fecha BETWEEN :desde AND :hasta
        AND pm.fechaBaja IS NULL
    """)
    fun findByFechaBetweenAndFechaBajaIsNull(
        @Param("desde") desde: LocalDateTime,
        @Param("hasta") hasta: LocalDateTime
    ): List<PagoMatricula>
}
