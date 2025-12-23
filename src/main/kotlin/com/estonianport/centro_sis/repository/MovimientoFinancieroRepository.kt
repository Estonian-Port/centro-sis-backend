package com.estonianport.centro_sis.repository

import com.estonianport.centro_sis.model.MovimientoFinanciero
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface MovimientoFinancieroRepository : CrudRepository<MovimientoFinanciero, Long> {
}