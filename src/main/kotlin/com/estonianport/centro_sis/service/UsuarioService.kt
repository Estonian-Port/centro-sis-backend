package com.estonianport.centro_sis.service

import com.estonianport.centro_sis.common.GenericServiceImpl
import com.estonianport.centro_sis.common.codeGeneratorUtil.CodeGeneratorUtil
import com.estonianport.centro_sis.model.Usuario
import com.estonianport.centro_sis.repository.UsuarioRepository
import com.estonianport.centro_sis.dto.request.UsuarioCambioPasswordRequestDto
import com.estonianport.centro_sis.dto.request.UsuarioRegistroRequestDto
import com.estonianport.centro_sis.model.enums.EstadoType
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.CrudRepository
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class UsuarioService : GenericServiceImpl<Usuario, Long>() {

    @Autowired
    lateinit var usuarioRepository: UsuarioRepository

    override val dao: CrudRepository<Usuario, Long>
        get() = usuarioRepository

    /*
        fun getAllUsuario(id : Long, pageNumber : Int): List<UsuarioAbmDTO> {
            return usuarioRepository.getAllUsuario(id, PageRequest.of(pageNumber,10)).content
        }

        fun getAllUsuarioFiltrados(id : Long, pageNumber : Int, buscar: String): List<UsuarioAbmDTO>{
            return usuarioRepository.getAllUsuarioFiltrados(id, buscar, PageRequest.of(pageNumber,10)).content
        }

        fun getCantidadUsuario(id : Long): Int {
            return usuarioRepository.getCantidadUsuario(id)
        }

        fun getCantidadUsuarioFiltrados(id : Long, buscar : String): Int {
            return usuarioRepository.getCantidadUsuarioFiltrados(id,buscar)
        }

    */
    fun getUsuarioByEmail(email: String): Usuario {
        return usuarioRepository.getUsuarioByEmail(email)
            ?: throw NoSuchElementException("No se encontró un usuario con el email proporcionado")
    }

    fun getUsuarioByCelular(celular: Long): Usuario? {
        return usuarioRepository.getUsuarioByCelular(celular)
    }

    fun findById(id: Long): Usuario? {
        return usuarioRepository.findById(id).get()
    }

    fun totalUsuarios(): Int {
        return usuarioRepository.totalUsuarios()
    }

    fun countUsuariosActivos(): Int {
        return usuarioRepository.countUsuariosActivos()
    }

    fun countUsuariosInactivos(): Int {
        return usuarioRepository.countUsuariosInactivos()
    }

    fun countUsuariosPendientes(): Int {
        return usuarioRepository.countUsuariosPendientes()
    }

    fun getUsuariosRegistrados(): List<Usuario> {
        //TODO ajustar usuario.rol. (ADMIN)
        return getAll()!!.filter { true && it.estado.name != "PENDIENTE" && it.estado.name != "BAJA" }
    }

    fun getUsuariosPendientes(): List<Usuario> {
        return getAll()!!.filter { it.estado.name == "PENDIENTE" }
    }

    fun verificarEmailNoExiste(email: String) {
        val usuario = usuarioRepository.getUsuarioByEmail(email)
        println(usuario?.email)
        if (usuario != null) {
            throw IllegalArgumentException("Ya existe un usuario registrado con el email proporcionado")
        }
    }

    fun encriptarPassword(password: String): String {
        return BCryptPasswordEncoder().encode(password)
    }

    fun generarPassword(): String {
        return CodeGeneratorUtil.base26Only4Letters + CodeGeneratorUtil.base26Only4Letters
    }

    fun actualizarFechaUltimoAcceso(email: String, fecha: LocalDate) {
        val usuario = getUsuarioByEmail(email)
        usuario.ultimoIngreso = fecha
        save(usuario)
    }

    fun primerLogin(usuarioDto: UsuarioRegistroRequestDto) {
        val usuario = findById(usuarioDto.id)
        if (usuario != null) {
            usuario.password = encriptarPassword(usuarioDto.nuevoPassword)
            usuario.nombre = usuarioDto.nombre
            usuario.apellido = usuarioDto.apellido
            usuario.celular = usuarioDto.celular
            usuario.confirmarPrimerLogin()
            save(usuario)
        }
    }

    fun darDeBaja(usuarioId: Long) {
        val usuario = findById(usuarioId) ?: throw NoSuchElementException("No se encontró un usuario con el ID proporcionado")
        usuario.estado = EstadoType.BAJA
        usuario.fechaBaja = LocalDate.now()
        save(usuario)
    }

    fun updatePerfil (usuario: Usuario) : Usuario {
        val usuarioExistente = findById(usuario.id) ?: throw NoSuchElementException("No se encontró un usuario con el ID proporcionado")
        usuarioExistente.nombre = usuario.nombre
        usuarioExistente.apellido = usuario.apellido
        usuarioExistente.email = usuario.email
        usuarioExistente.celular = usuario.celular
        save(usuarioExistente)
        return usuarioExistente
    }

    fun updatePassword (usuarioDto : UsuarioCambioPasswordRequestDto) : Usuario {
        val usuario = getUsuarioByEmail(usuarioDto.email)
        if (!BCryptPasswordEncoder().matches(usuarioDto.passwordActual, usuario.password)) {
            throw IllegalArgumentException("La contraseña actual es incorrecta")
        }
        if (usuarioDto.nuevoPassword != usuarioDto.confirmacionPassword) {
            throw IllegalArgumentException("La confirmación de la nueva contraseña no coincide")
        }
        if (BCryptPasswordEncoder().matches(usuarioDto.nuevoPassword, usuario.password)) {
            throw IllegalArgumentException("La nueva contraseña no puede ser igual a la actual")
        }
        usuario.password = encriptarPassword(usuarioDto.nuevoPassword)
        save(usuario)
        return usuario
    }

}