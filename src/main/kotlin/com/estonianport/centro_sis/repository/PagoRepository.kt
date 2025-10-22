package com.estonianport.centro_sis.repository

import com.estonianport.centro_sis.model.Pago
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface PagoRepository : CrudRepository<Pago, Long> {
}