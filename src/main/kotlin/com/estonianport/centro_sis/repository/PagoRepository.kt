package com.estonianport.centro_sis.repository

import com.estonianport.centro_sis.model.*
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
interface PagoRepository : JpaRepository<Pago, Long> {

    // ========================================
    // QUERIES SIMPLES - SIN JPQL
    // ========================================

    /**
     * Pagos CURSO de cursos COMISION (activos)
     */
    fun findAllByFechaBajaIsNullAndIdIn(ids: List<Long>, pageable: Pageable): Page<Pago>

    /**
     * Todos los pagos activos
     */
    fun findAllByFechaBajaIsNull(pageable: Pageable): Page<Pago>
}