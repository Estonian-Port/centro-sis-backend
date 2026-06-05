package com.estonianport.centro_sis.repository

import com.estonianport.centro_sis.model.ConfiguracionMatricula
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ConfiguracionMatriculaRepository : JpaRepository<ConfiguracionMatricula, Long> {
    fun findByAnio(anio: Int): ConfiguracionMatricula?
}
