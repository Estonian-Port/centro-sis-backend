package com.estonianport.centro_sis.repository

import com.estonianport.centro_sis.model.Curso
import com.estonianport.centro_sis.model.Rol
import com.estonianport.centro_sis.model.RolProfesor
import com.estonianport.centro_sis.model.enums.EstadoType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface RolRepository : JpaRepository<Rol, Long> {

    @Query(
        """
        SELECT COUNT(DISTINCT r.usuario.id)
        FROM Rol r
        WHERE TYPE(r) = com.estonianport.centro_sis.model.RolAlumno
        AND r.usuario.estado = :estado
        """
    )
    fun countDistinctUsuariosAlumnoByEstado(estado: EstadoType): Long

    @Query(
        """
        SELECT COUNT(DISTINCT r.usuario.id)
        FROM Rol r
        WHERE TYPE(r) = com.estonianport.centro_sis.model.RolProfesor
        AND r.usuario.estado = :estado
        """
    )
    fun countDistinctUsuariosProfesorByEstado(estado: EstadoType): Long

    fun findRolProfesorByCursos_Id(cursoId: Long): List<RolProfesor>

    @Query("select r.cursos from RolProfesor r where r.usuario.id = :usuarioId")
    fun getCursosByProfesorUsuarioId(@Param("usuarioId") usuarioId: Long): List<Curso>

}