package com.estonianport.centro_sis.repository

import com.estonianport.centro_sis.model.Pago
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
interface PagoRepository : CrudRepository<Pago, Long> {
    @Query(
        """
    SELECT p
    FROM Pago p
    JOIN p.inscripcion i
    JOIN i.alumno a
    JOIN a.usuario u
    WHERE u.id = :usuarioId 
    AND p.fechaBaja IS NULL
    """
    )
    fun getAllByUsuarioId(usuarioId: Long): List<Pago>

    fun findByFechaBetweenAndFechaBajaIsNull(desde: LocalDate, hasta: LocalDate): List<Pago>

}