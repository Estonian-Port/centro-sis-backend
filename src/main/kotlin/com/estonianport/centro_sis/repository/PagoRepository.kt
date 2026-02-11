package com.estonianport.centro_sis.repository

import com.estonianport.centro_sis.model.*
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface PagoRepository : JpaRepository<Pago, Long> {

    /**
     * Todos los pagos activos
     */
    fun findAllByFechaBajaIsNull(pageable: Pageable): Page<Pago>

}