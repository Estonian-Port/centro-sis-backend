package com.estonianport.centro_sis.repository

import com.estonianport.centro_sis.model.Pago
import com.estonianport.centro_sis.model.PagoAlquiler
import com.estonianport.centro_sis.model.PagoComision
import com.estonianport.centro_sis.model.PagoCurso
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
interface PagoRepository : JpaRepository<Pago, Long> {

    fun findByFechaBetweenAndFechaBajaIsNull(desde: LocalDate, hasta: LocalDate): List<Pago>

    fun findByFechaBajaIsNull(): List<Pago>

}

// Repositorios específicos si necesitas queries específicas
interface PagoInscripcionRepository : JpaRepository<PagoCurso, Long> {
    fun findByInscripcionAlumnoId(alumnoId: Long): List<PagoCurso>
    fun findByInscripcionId(inscripcionId: Long): List<PagoCurso>
}

interface PagoAlquilerRepository : JpaRepository<PagoAlquiler, Long> {
    fun findByProfesorId(profesorId: Long): List<PagoAlquiler>
    fun findByCursoId(cursoId: Long): List<PagoAlquiler>
}

interface PagoComisionRepository : JpaRepository<PagoComision, Long> {
    fun findByProfesorId(profesorId: Long): List<PagoComision>
}