package com.estonianport.centro_sis.repository

import com.estonianport.centro_sis.model.Pago
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface PagoRepository : CrudRepository<Pago, Long> {
    @Query(
        """
        SELECT p
        FROM Pago p
        WHERE p.alumno.id = :id 
        AND p.fechaBaja IS NULL
        """
    )
    fun getAllByUsuarioId(id: Long): List<Pago>
}