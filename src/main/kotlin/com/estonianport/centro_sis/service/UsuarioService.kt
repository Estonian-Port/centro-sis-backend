package com.estonianport.centro_sis.service

import com.estonianport.centro_sis.common.GenericServiceImpl
import com.estonianport.centro_sis.common.codeGeneratorUtil.CodeGeneratorUtil
import com.estonianport.centro_sis.common.emailService.EmailService
import com.estonianport.centro_sis.common.errors.ConflictException
import com.estonianport.centro_sis.dto.request.UsuarioAltaRequestDto
import com.estonianport.centro_sis.dto.request.UsuarioCambioPasswordRequestDto
import com.estonianport.centro_sis.dto.request.UsuarioRegistroRequestDto
import com.estonianport.centro_sis.dto.request.UsuarioUpdatePerfilRequestDto
import com.estonianport.centro_sis.dto.response.UsuarioResponseDto
import com.estonianport.centro_sis.mapper.UsuarioMapper
import com.estonianport.centro_sis.model.AdultoResponsable
import com.estonianport.centro_sis.model.RolFactory
import com.estonianport.centro_sis.model.RolProfesor
import com.estonianport.centro_sis.model.Usuario
import com.estonianport.centro_sis.model.enums.EstadoType
import com.estonianport.centro_sis.model.enums.RolType
import com.estonianport.centro_sis.model.enums.TipoAcceso
import com.estonianport.centro_sis.repository.RolRepository
import com.estonianport.centro_sis.repository.UsuarioRepository
import jakarta.transaction.Transactional
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.cache.annotation.Caching
import org.springframework.data.repository.CrudRepository
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.LocalDateTime

@Service
class UsuarioService : GenericServiceImpl<Usuario, Long>() {

    @Autowired lateinit var usuarioRepository: UsuarioRepository
    @Autowired lateinit var rolRepository: RolRepository
    @Autowired lateinit var emailService: EmailService
    @Autowired lateinit var rolFactory: RolFactory

    override val dao: CrudRepository<Usuario, Long>
        get() = usuarioRepository

    // ─── LECTURAS ─────────────────────────────────────────────────────────────

    /**
     * Carga la entidad desde DB. No cacheable aquí porque el caché de entidades
     * JPA con lazy collections provoca el Document nesting depth (1001).
     * El caché se aplica en la capa DTO (buildUsuarioResponseDto).
     */
    fun getById(id: Long): Usuario =
        usuarioRepository.findById(id)
            .orElseThrow { NoSuchElementException("No se encontró un usuario con el ID proporcionado") }

    fun findById(id: Long): Usuario? =
        usuarioRepository.findById(id).orElse(null)

    fun getUsuarioByEmail(email: String): Usuario =
        usuarioRepository.getUsuarioByEmail(email)
            ?: throw NoSuchElementException("No se encontró un usuario con el email proporcionado")

    /**
     * Retorna DTOs cacheados para el listado general. Se invalida cuando se
     * persiste o modifica cualquier usuario.
     */
    @Cacheable(value = ["usuarios:lista"], key = "'activos'")
    fun getAllUsuariosDto(): List<UsuarioResponseDto> =
        usuarioRepository.findAllActivos()
            .map { UsuarioMapper.buildUsuarioResponseDto(it) }

    fun getAllUsuarios(): Set<Usuario> =
        usuarioRepository.findAllActivos()

    @Cacheable(value = ["usuarios:lista"], key = "'activos-excepto-' + #userId")
    fun getAllActivosExceptoDto(userId: Long): List<UsuarioResponseDto> =
        usuarioRepository.getAllActivosExcepto(userId)
            .map { UsuarioMapper.buildUsuarioResponseDto(it) }

    /** Mantener por compatibilidad con código existente que recibe Set<Usuario> */
    fun getAllActivosExcepto(userId: Long): Set<Usuario> =
        usuarioRepository.getAllActivosExcepto(userId)

    @Cacheable(value = ["usuarios:rol"], key = "#rolTipo.name()")
    fun getUsuariosPorRol(rolTipo: RolType): List<Usuario> =
        when (rolTipo) {
            RolType.PROFESOR      -> usuarioRepository.findProfesores()
            RolType.ALUMNO        -> usuarioRepository.findAlumnos()
            RolType.ADMINISTRADOR -> usuarioRepository.findAdministradores()
            RolType.OFICINA       -> usuarioRepository.findOficina()
            RolType.PORTERIA      -> usuarioRepository.findPorteria()
        }

    /**
     * Búsqueda delegada a la base de datos: no carga toda la tabla en memoria.
     * El limit se aplica después porque JPQL no acepta LIMIT directamente en todos
     * los providers; para volumen alto se puede agregar Pageable.
     */
    fun searchByRol(q: String, rolTipo: RolType, limit: Int = 20): List<Usuario> {
        val resultados = when (rolTipo) {
            RolType.ALUMNO   -> usuarioRepository.searchAlumnos(q)
            RolType.PROFESOR -> usuarioRepository.searchProfesores(q)
            else             -> usuarioRepository.searchTodos(q)
                .filter { it.tieneRol(rolTipo) }
        }
        return resultados.take(limit.coerceAtMost(50))
    }

    fun searchTodos(q: String, limit: Int = 20): List<Usuario> =
        usuarioRepository.searchTodos(q).take(limit.coerceAtMost(50))

    // ─── ESCRITURAS ───────────────────────────────────────────────────────────

    @Transactional
    @Caching(evict = [
        CacheEvict(value = ["usuarios:lista"], allEntries = true),
        CacheEvict(value = ["usuarios:rol"],   allEntries = true),
    ])
    fun altaUsuario(usuarioDto: UsuarioAltaRequestDto): Usuario {
        val usuario = usuarioRepository.getUsuarioByEmail(usuarioDto.email)
        if (usuario != null && usuario.estado != EstadoType.BAJA) {
            throw ConflictException("Ya existe un usuario registrado con el email proporcionado")
        }
        return if (usuario != null && usuario.estado == EstadoType.BAJA) {
            reactivarUsuario(usuario, usuarioDto.roles)
        } else {
            crearUsuario(usuarioDto)
        }
    }

    @Transactional
    @Caching(evict = [
        CacheEvict(value = ["usuarios:lista"], allEntries = true),
        CacheEvict(value = ["usuarios:rol"],   allEntries = true),
    ])
    fun reactivarUsuario(usuario: Usuario, roles: List<String>): Usuario {
        usuario.estado = EstadoType.PENDIENTE
        usuario.fechaBaja = null
        usuario.ultimoIngresoAlSistema = null
        val password = generarPassword()
        usuario.password = encriptarPassword(password)
        usuario.fechaAlta = LocalDate.now()
        roles.forEach {
            val rol = rolFactory.build(it, usuario)
            usuario.asignarRol(rol)
        }
        enviarEmailBienvenida(usuario, password)
        return save(usuario)
    }

    @Transactional
    @Caching(evict = [
        CacheEvict(value = ["usuarios:lista"], allEntries = true),
        CacheEvict(value = ["usuarios:rol"],   allEntries = true),
    ])
    fun crearUsuario(usuarioDto: UsuarioAltaRequestDto): Usuario {
        val usuario = UsuarioMapper.buildAltaUsuario(usuarioDto.email)
        val password = generarPassword()
        usuario.password = encriptarPassword(password)
        usuario.fechaAlta = LocalDate.now()
        usuarioDto.roles.forEach {
            val rol = rolFactory.build(it, usuario)
            usuario.asignarRol(rol)
        }
        val usuarioNuevo = save(usuario)
        enviarEmailBienvenida(usuarioNuevo, password)
        return usuarioNuevo
    }

    @Transactional
    @Caching(evict = [
        CacheEvict(value = ["usuarios:lista"], allEntries = true),
        CacheEvict(value = ["usuarios:rol"],   allEntries = true),
    ])
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

    @Transactional
    @Caching(evict = [
        CacheEvict(value = ["usuarios:lista"], allEntries = true),
    ])
    fun darDeBaja(usuarioId: Long, eliminadoPorId: Long) {
        puedeEliminar(eliminadoPorId)
        val usuario = findById(usuarioId)
            ?: throw NoSuchElementException("No se encontró un usuario con el ID proporcionado")
        usuario.estado = EstadoType.BAJA
        usuario.fechaBaja = LocalDate.now()
        save(usuario)
    }

    @Transactional
    @Caching(evict = [
        CacheEvict(value = ["usuarios:lista"], allEntries = true),
    ])
    fun updatePerfil(usuario: UsuarioUpdatePerfilRequestDto, id: Long): Usuario {
        val usuarioExistente = findById(id)
            ?: throw NoSuchElementException("No se encontró un usuario con el ID proporcionado")
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

    @Transactional
    fun updatePassword(usuarioDto: UsuarioCambioPasswordRequestDto, id: Long): Usuario {
        val usuario = getById(id)
        if (!BCryptPasswordEncoder().matches(usuarioDto.passwordActual, usuario.password)) {
            throw ConflictException("La contraseña actual es incorrecta")
        }
        if (usuarioDto.nuevoPassword != usuarioDto.confirmacionPassword) {
            throw ConflictException("La confirmación de la nueva contraseña no coincide")
        }
        if (BCryptPasswordEncoder().matches(usuarioDto.nuevoPassword, usuario.password)) {
            throw ConflictException("La nueva contraseña no puede ser igual a la actual")
        }
        usuario.password = encriptarPassword(usuarioDto.nuevoPassword)
        save(usuario)
        return usuario
    }

    @Transactional
    @Caching(evict = [
        CacheEvict(value = ["usuarios:lista"], allEntries = true),
        CacheEvict(value = ["usuarios:rol"],   allEntries = true),
    ])
    fun solicitarRecuperarPassword(email: String) {
        val usuario = usuarioRepository.getUsuarioByEmail(email) ?: return
        if (usuario.estado == EstadoType.BAJA) return
        val nuevaPassword = generarPassword()
        usuario.password = encriptarPassword(nuevaPassword)
        usuario.estado = EstadoType.PENDIENTE
        usuario.ultimoIngresoAlSistema = null
        save(usuario)
        try {
            emailService.enviarEmailRecuperarPassword(usuario, nuevaPassword)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @Transactional
    @Caching(evict = [
        CacheEvict(value = ["usuarios:lista"], allEntries = true),
        CacheEvict(value = ["usuarios:rol"],   allEntries = true),
    ])
    fun registrarAccesoManual(idUsuario: Long) {
        val usuario = getById(idUsuario)
        usuario.registrarAcceso(TipoAcceso.MANUAL)
        save(usuario)
    }

    @Transactional
    fun actualizarEstadoProfesor(profesores: MutableSet<RolProfesor>) {
        profesores.forEach { it.actualizarEstado() }
        profesores.forEach { save(it.usuario) }
    }

    @Transactional
    fun obtenerVariosPorId(ids: List<Long>): List<Usuario> {
        if (ids.isEmpty()) return emptyList()
        val usuarios = usuarioRepository.findAllWithRolesByIds(ids)
        if (usuarios.size != ids.distinct().size) {
            throw NoSuchElementException("Uno o más IDs de usuario proporcionados no existen.")
        }
        return usuarios
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    fun countAlumnosActivos(): Long =
        rolRepository.countDistinctUsuariosAlumnoByEstado(EstadoType.ACTIVO)

    fun countProfesores(): Long =
        rolRepository.countDistinctUsuariosProfesorByEstado(EstadoType.ACTIVO)

    fun puedeEliminar(eliminadoPorId: Long) {
        val usuarioEliminador = getById(eliminadoPorId)
        if (!usuarioEliminador.tieneRol(RolType.ADMINISTRADOR) &&
            !usuarioEliminador.tieneRol(RolType.OFICINA)) {
            throw ConflictException("El usuario no tiene permisos para eliminar usuarios")
        }
    }

    fun verificarEmailEnUso(email: String, id: Long) {
        val usuario = usuarioRepository.getUsuarioByEmail(email)
        if (usuario != null && usuario.id != id) {
            throw ConflictException("Ya existe un usuario registrado con el email proporcionado")
        }
    }

    fun verficarDniNoExiste(dni: String, id: Long) {
        // Idealmente agregar findByDni al repository para no traer todos
        val usuario = usuarioRepository.findAll().find { it.dni == dni }
        if (usuario != null && usuario.id != id) {
            throw ConflictException("Ya existe un usuario registrado con el DNI proporcionado")
        }
    }

    fun enviarEmailBienvenida(usuario: Usuario, password: String) {
        try {
            emailService.enviarEmailAltaUsuario(usuario, "Bienvenido a CENTRO SIS", password)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun encriptarPassword(password: String): String =
        BCryptPasswordEncoder().encode(password)

    fun generarPassword(): String =
        CodeGeneratorUtil.base26Only4Letters + CodeGeneratorUtil.base26Only4Letters

    fun actualizarFechaUltimoAcceso(email: String, fecha: LocalDateTime) {
        val usuario = getUsuarioByEmail(email)
        usuario.ultimoIngresoAlSistema = fecha
        save(usuario)
    }
}