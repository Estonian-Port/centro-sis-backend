package com.estonianport.centro_sis.repository

import com.estonianport.centro_sis.model.Usuario
import com.estonianport.centro_sis.model.enums.EstadoType
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface UsuarioRepository : CrudRepository<Usuario, Long> {

    fun findOneByEmail(email: String): Usuario?

    // TODO una vez leidos y entendido, borrar los comentarios, y hay q arreglar si vamos a usar "usuarios"
    // o "usuario", al poner "usuarios" seria como decir "all usuario" o "lista usuario" me da igual usar cualquiera de los dos

    // TODO Creo que ahi estaria, tiene q ser fecha ultimo ingreso not null, fecha baja null y tener asignada
    //TODO Ajustar IS ADMIN con ROL
    @Query(
        """
        SELECT COUNT(DISTINCT u)  
        FROM Usuario u
        WHERE u.ultimoIngreso IS NOT NULL
        AND u.fechaBaja IS NULL

    """
    )
    fun countUsuariosActivos(): Int

    //TODO Ajustar IS ADMIN con ROL
    @Query(
        """
    SELECT COUNT(DISTINCT u)
    FROM Usuario u
    WHERE u.ultimoIngreso IS NOT NULL
    AND u.fechaBaja IS NULL
"""
    )
    fun countUsuariosInactivos(): Int

    //TODO Ajustar IS ADMIN con ROL
    @Query(
        """
    SELECT COUNT(u)
    FROM Usuario u
    WHERE u.fechaBaja IS NULL
    AND u.ultimoIngreso IS NULL
"""
    )
    fun countUsuariosPendientes(): Int

    //TODO Ajustar IS ADMIN con ROL
    @Query(
        """
        SELECT COUNT(u) 
        FROM Usuario u
    """
    )
    fun totalUsuarios(): Int

    // TODO y aca dejo la q devolveria usuarios, seria optimo q si la vas a usar solo para listar en front
    // devuelva usuarioDto
    @Query(
        """
        SELECT DISTINCT u
        FROM Usuario u
        WHERE u.ultimoIngreso IS NOT NULL
        AND u.fechaBaja IS NULL
    """
    )
    fun getUsuariosActivos(): List<Usuario>

    /*
    @Query("SELECT new com.estonianport.unique.dto.UsuarioAbmDTO(c.usuario.id, c.usuario.nombre, " +
            "c.usuario.apellido, c.usuario.username) FROM Cargo c WHERE c.empresa.id = :empresaId AND " +
            "c.fechaBaja IS NULL")
    fun getAllUsuario(empresaId: Long, pageable: PageRequest) : Page<UsuarioAbmDTO>

    @Query("SELECT new com.estonianport.agendaza.dto.UsuarioAbmDTO(c.usuario.id, c.usuario.nombre, " +
            "c.usuario.apellido, c.usuario.username) FROM Cargo c WHERE c.empresa.id = :empresaId " +
            "AND (c.usuario.nombre ILIKE %:buscar% OR c.usuario.apellido ILIKE %:buscar%) AND c.fechaBaja IS NULL")
    fun getAllUsuarioFiltrados(empresaId : Long, buscar : String, pageable : Pageable) : Page<UsuarioAbmDTO>

    @Query("SELECT COUNT(c) FROM Cargo c WHERE c.empresa.id = :empresaId AND c.fechaBaja IS NULL")
    fun getCantidadUsuario(empresaId : Long) : Int

    @Query("SELECT COUNT(c) FROM Cargo c WHERE c.empresa.id = :empresaId AND " +
            "(c.usuario.nombre ILIKE %:buscar% OR c.usuario.apellido ILIKE %:buscar%) AND c.fechaBaja IS NULL")
    fun getCantidadUsuarioFiltrados(empresaId : Long, buscar: String) : Int
    */

    fun getUsuarioByEmail(email: String): Usuario?

    fun getUsuarioByCelular(celular: Long): Usuario?

    override fun findById(id: Long): Optional<Usuario>
}