package com.estonianport.centro_sis.service

import com.estonianport.centro_sis.common.GenericServiceImpl
import com.estonianport.centro_sis.common.codeGeneratorUtil.CodeGeneratorUtil
import com.estonianport.centro_sis.model.Usuario
import com.estonianport.centro_sis.repository.UsuarioRepository
import com.estonianport.centro_sis.dto.request.UsuarioCambioPasswordRequestDto
import com.estonianport.centro_sis.dto.request.UsuarioRegistroRequestDto
import com.estonianport.centro_sis.dto.request.UsuarioUpdatePerfilRequestDto
import com.estonianport.centro_sis.model.AdultoResponsable
import com.estonianport.centro_sis.model.Curso
import com.estonianport.centro_sis.model.Inscripcion
import com.estonianport.centro_sis.model.enums.EstadoType
import com.estonianport.centro_sis.model.enums.RolType
import com.estonianport.centro_sis.model.enums.TipoAcceso
import com.estonianport.centro_sis.repository.RolRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.CrudRepository
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.LocalDateTime

@Service
class UsuarioService : GenericServiceImpl<Usuario, Long>() {

    @Autowired
    lateinit var usuarioRepository: UsuarioRepository

    @Autowired
    lateinit var rolRepository: RolRepository

    override val dao: CrudRepository<Usuario, Long>
        get() = usuarioRepository

    fun getById(id: Long): Usuario {
        return usuarioRepository.findById(id)
            .orElseThrow { NoSuchElementException("No se encontró un usuario con el ID proporcionado") }
    }

    fun getAllUsuarios(): List<Usuario> {
        return usuarioRepository.findAllByOrderByNombreAsc()
    }

    fun getUsuarioByEmail(email: String): Usuario {
        return usuarioRepository.getUsuarioByEmail(email)
            ?: throw NoSuchElementException("No se encontró un usuario con el email proporcionado")
    }

    fun findById(id: Long): Usuario? {
        return usuarioRepository.findById(id).get()
    }

    fun verificarEmailNoExistente(email: String) {
        val usuario = usuarioRepository.getUsuarioByEmail(email)
        if (usuario != null) {
            throw IllegalArgumentException("Ya existe un usuario registrado con el email proporcionado")
        }
    }

    fun verificarEmailEnUso(email: String, id: Long) {
        val usuario = usuarioRepository.getUsuarioByEmail(email)
        if (usuario != null && usuario.id != id) {
            throw IllegalArgumentException("Ya existe un usuario registrado con el email proporcionado")
        }
    }

    fun verficarDniNoExiste(dni: String, id: Long) {
        val usuario = usuarioRepository.findAll().toList().find { it.dni == dni }
        if (usuario != null && usuario.id != id) {
            throw IllegalArgumentException("Ya existe un usuario registrado con el DNI proporcionado")
        }
    }

    fun encriptarPassword(password: String): String {
        return BCryptPasswordEncoder().encode(password)
    }

    fun generarPassword(): String {
        return CodeGeneratorUtil.base26Only4Letters + CodeGeneratorUtil.base26Only4Letters
    }

    fun actualizarFechaUltimoAcceso(email: String, fecha: LocalDateTime) {
        val usuario = getUsuarioByEmail(email)
        usuario.ultimoIngresoAlSistema = fecha
        save(usuario)
    }

    fun primerLogin(usuarioDto: UsuarioRegistroRequestDto): Usuario {
        val usuario = getById(usuarioDto.id)
        usuario.password = encriptarPassword(usuarioDto.password)
        usuario.nombre = usuarioDto.nombre
        usuario.apellido = usuarioDto.apellido
        usuario.dni = usuarioDto.dni
        usuario.celular = usuarioDto.celular
        usuario.fechaNacimiento = usuarioDto.fechaNacimiento
        usuario.confirmarPrimerLogin()
        if (usuarioDto.adultoResponsable != null) {
            usuario.adultoResponsable = AdultoResponsable(
                nombre = usuarioDto.adultoResponsable.nombre,
                apellido = usuarioDto.adultoResponsable.apellido,
                dni = usuarioDto.adultoResponsable.dni,
                celular = usuarioDto.adultoResponsable.celular,
                relacion = usuarioDto.adultoResponsable.relacion
            )
        }
        return save(usuario)
    }

    fun darDeBaja(usuarioId: Long) {
        val usuario =
            findById(usuarioId) ?: throw NoSuchElementException("No se encontró un usuario con el ID proporcionado")
        usuario.estado = EstadoType.BAJA
        usuario.fechaBaja = LocalDate.now()
        save(usuario)
    }

    fun updatePerfil(usuario: UsuarioUpdatePerfilRequestDto, id: Long): Usuario {
        val usuarioExistente =
            findById(id) ?: throw NoSuchElementException("No se encontró un usuario con el ID proporcionado")
        usuarioExistente.nombre = usuario.nombre
        usuarioExistente.apellido = usuario.apellido
        verficarDniNoExiste(usuario.dni, id)
        usuarioExistente.dni = usuario.dni
        verificarEmailEnUso(usuario.email, id)
        usuarioExistente.email = usuario.email
        usuarioExistente.celular = usuario.celular
        save(usuarioExistente)
        return usuarioExistente
    }

    fun updatePassword(usuarioDto: UsuarioCambioPasswordRequestDto, id: Long): Usuario {
        val usuario = getById(id)
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

    fun countAlumnosActivos(): Long {
        return rolRepository.countDistinctUsuariosAlumnoByEstado(EstadoType.ACTIVO)
    }

    fun countProfesores(): Long {
        return rolRepository.countDistinctUsuariosProfesorByEstado(EstadoType.ACTIVO)
    }

    fun obtenerInscripcionesPorAlumno(idAlumno: Long): List<Inscripcion> {
        val usuario = getById(idAlumno)
        if (!usuario.tieneRol(RolType.ALUMNO)) {
            throw IllegalArgumentException("El usuario con ID $idAlumno no es un alumno.")
        }
        return usuario.getRolAlumno().getInscripcionesActivas()
    }

    fun obtenerCursosProfesor(idProfe: Long): List<Curso> {
        val usuario = getById(idProfe)
        if (!usuario.tieneRol(RolType.PROFESOR)) {
            throw IllegalArgumentException("El usuario con ID $idProfe no es un profesor.")
        }
        return usuario.getRolProfesor().cursosActivos()
    }

    fun getUsuariosPorRol(rolTipo: RolType): List<Usuario> {
        return when (rolTipo) {
            RolType.PROFESOR -> usuarioRepository.findProfesores()
            RolType.ALUMNO -> usuarioRepository.findAlumnos()
            RolType.ADMINISTRADOR -> usuarioRepository.findAdministradores()
            RolType.OFICINA -> usuarioRepository.findOficina()
            RolType.PORTERIA -> usuarioRepository.findPorteria()
        }
    }

    fun registrarAccesoManual(idUsuario: Long) {
        val usuario = getById(idUsuario)
        usuario.registrarAcceso(TipoAcceso.MANUAL)
        save(usuario)
    }

}