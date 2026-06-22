package com.estonianport.centro_sis.service

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
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.cache.annotation.Caching
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.LocalDateTime

@Service
class UsuarioService(
    private val usuarioRepository: UsuarioRepository,
    private val rolRepository: RolRepository,
    private val emailService: EmailService,
    private val rolFactory: RolFactory
) {

    // ─── LECTURAS ─────────────────────────────────────────────────────────────

    fun getById(id: Long): Usuario =
        usuarioRepository.findById(id)
            .orElseThrow { NoSuchElementException("No se encontró un usuario con el ID proporcionado") }

    fun findById(id: Long): Usuario? =
        usuarioRepository.findById(id).orElse(null)

    fun getUsuarioByEmail(email: String): Usuario =
        usuarioRepository.getUsuarioByEmail(email)
            ?: throw NoSuchElementException("No se encontró un usuario con el email proporcionado")

    @Cacheable(value = ["usuarios:lista"], key = "'activos-excepto-' + #userId")
    fun getAllActivosExceptoDto(userId: Long): List<UsuarioResponseDto> =
        usuarioRepository.getAllActivosExcepto(userId)
            .map { UsuarioMapper.buildUsuarioResponseDto(it) }

    fun getAllActivosExceptoPaginado(
        userId: Long,
        page: Int,
        size: Int,
        search: String?,
        roles: List<RolType>?,
        estados: List<EstadoType>?
    ): Page<UsuarioResponseDto> {
        val pageable = PageRequest.of(page, size)

        val idsPage: Page<Long> = usuarioRepository.findIdsActivosExcepto(
            userId,
            search?.takeIf { it.isNotBlank() },
            pageable
        )

        if (idsPage.isEmpty) return Page.empty(pageable)

        val usuariosMap = usuarioRepository.findWithRolesByIds(idsPage.content)
            .associateBy { it.id }

        var usuarios = idsPage.content.mapNotNull { usuariosMap[it] }

        if (!roles.isNullOrEmpty()) {
            usuarios = usuarios.filter { u -> roles.any { r -> u.tieneRol(r) } }
        }
        if (!estados.isNullOrEmpty()) {
            usuarios = usuarios.filter { u -> estados.contains(u.estado) }
        }

        val dtos = usuarios.map { UsuarioMapper.buildUsuarioResponseDto(it) }

        return PageImpl(dtos, pageable, idsPage.totalElements)
    }

    @Cacheable(value = ["usuarios:rol"], key = "#rolTipo.name()")
    fun getUsuariosPorRol(rolTipo: RolType): List<Usuario> =
        when (rolTipo) {
            RolType.PROFESOR      -> usuarioRepository.findProfesores()
            RolType.ALUMNO        -> usuarioRepository.findAlumnos()
            RolType.ADMINISTRADOR -> usuarioRepository.findAdministradores()
            RolType.OFICINA       -> usuarioRepository.findOficina()
            RolType.PORTERIA      -> usuarioRepository.findPorteria()
        }

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
    fun save(usuario: Usuario): Usuario {
        return usuarioRepository.save(usuario)
    }

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
        CacheEvict(value = ["usuarios:rol"],   allEntries = true),
        CacheEvict(value = ["cursos:detalle"], allEntries = true),
        CacheEvict(value = ["cursos:resumen:pagina"], allEntries = true),
        CacheEvict(value = ["cursos:resumen:activos"], allEntries = true),
        CacheEvict(value = ["cursos:resumen:profesor"], allEntries = true),
        CacheEvict(value = ["cursos:profesor:summary"], allEntries = true)
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
        CacheEvict(value = ["cursos:detalle"], allEntries = true),
        CacheEvict(value = ["cursos:resumen:pagina"], allEntries = true),
        CacheEvict(value = ["cursos:resumen:activos"], allEntries = true),
        CacheEvict(value = ["cursos:resumen:profesor"], allEntries = true),
        CacheEvict(value = ["cursos:profesor:summary"], allEntries = true)
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
    @Caching(evict = [
        CacheEvict(value = ["usuarios:lista"], allEntries = true),
        CacheEvict(value = ["usuarios:rol"],   allEntries = true),
    ])
    fun actualizarEstadoProfesor(profesores: MutableSet<RolProfesor>) {
        profesores.forEach { it.actualizarEstado() }
        profesores.forEach { save(it.usuario) }
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

    fun existsByDni(dni: String): Boolean {
        return usuarioRepository.existsByDni(dni)
    }
}