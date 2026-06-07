package com.estonianport.centro_sis.controller

import com.estonianport.centro_sis.common.emailService.EmailService
import com.estonianport.centro_sis.dto.request.AltaAlumnoRequestDto
import com.estonianport.centro_sis.dto.request.RecuperarPasswordRequestDto
import com.estonianport.centro_sis.dto.request.UsuarioAltaRequestDto
import com.estonianport.centro_sis.dto.request.UsuarioCambioPasswordRequestDto
import com.estonianport.centro_sis.dto.request.UsuarioRegistroRequestDto
import com.estonianport.centro_sis.dto.request.UsuarioRequestDto
import com.estonianport.centro_sis.dto.request.UsuarioUpdatePerfilRequestDto
import com.estonianport.centro_sis.dto.response.CursoAlumnoResponseDto
import com.estonianport.centro_sis.dto.response.CursoResponseDto
import com.estonianport.centro_sis.dto.response.CustomResponse
import com.estonianport.centro_sis.mapper.CursoMapper
import com.estonianport.centro_sis.mapper.PagoMapper
import com.estonianport.centro_sis.mapper.UsuarioMapper
import com.estonianport.centro_sis.model.RolFactory
import com.estonianport.centro_sis.model.enums.EstadoType
import com.estonianport.centro_sis.model.enums.RolType
import com.estonianport.centro_sis.repository.UsuarioRepository
import com.estonianport.centro_sis.service.AdministracionService
import com.estonianport.centro_sis.service.CursoService
import com.estonianport.centro_sis.service.InscripcionService
import com.estonianport.centro_sis.service.UsuarioService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import java.security.Principal

@RestController
@RequestMapping("/usuario")
@CrossOrigin("*")
class UsuarioController(
    private val rolFactory: RolFactory,
) {
    @Autowired private lateinit var usuarioRepository: UsuarioRepository
    @Autowired private lateinit var inscripcionService: InscripcionService
    @Autowired private lateinit var emailService: EmailService
    @Autowired lateinit var administracionService: AdministracionService
    @Autowired lateinit var usuarioService: UsuarioService
    @Autowired lateinit var cursoService: CursoService

    // ─── Perfil propio ────────────────────────────────────────────────────────

    @GetMapping("/me")
    fun getCurrent(principal: Principal): ResponseEntity<CustomResponse> {
        val usuario = usuarioService.getUsuarioByEmail(principal.name)
        return ok("Usuario obtenido correctamente", UsuarioMapper.buildUsuarioResponseDto(usuario))
    }

    // ─── Alta ─────────────────────────────────────────────────────────────────

    @PostMapping("/altaUsuario")
    fun altaUsuario(@RequestBody usuarioDto: UsuarioAltaRequestDto): ResponseEntity<CustomResponse> =
        ok("Usuario creado correctamente",
            UsuarioMapper.buildUsuarioResponseDto(usuarioService.altaUsuario(usuarioDto)))

    /**
     * Alta pública de alumno (sin autenticación).
     * Validaciones de negocio movidas al service; aquí solo validaciones de formato.
     */
    @PostMapping("/altaAlumno")
    fun altaAlumno(@RequestBody request: AltaAlumnoRequestDto): ResponseEntity<CustomResponse> {
        // Validaciones de formato (sin tocar la DB)
        if (!request.dni.matches(Regex("^\\d{7,8}$"))) {
            return error(HttpStatus.BAD_REQUEST, "El DNI debe tener 7 u 8 dígitos numéricos")
        }
        if (!request.email.matches(Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"))) {
            return error(HttpStatus.BAD_REQUEST, "El formato del email no es válido")
        }

        // Validaciones de negocio contra DB
        val usuarioConEmail = usuarioRepository.getUsuarioByEmail(request.email)
        if (usuarioConEmail != null && usuarioConEmail.estado != EstadoType.BAJA) {
            return error(HttpStatus.CONFLICT,
                "Ya existe un usuario registrado con el email proporcionado")
        }
        if (usuarioConEmail != null && usuarioConEmail.estado == EstadoType.BAJA) {
            return error(HttpStatus.CONFLICT,
                "Tu email estaba asociado a una cuenta dada de baja. " +
                        "Si deseas reactivar tu cuenta, por favor contactá a la administración.")
        }
        if (usuarioRepository.existsByDni(request.dni)) {
            return error(HttpStatus.CONFLICT,
                "Ya existe un usuario registrado con ese DNI. " +
                        "Si olvidaste tu contraseña, contactá a la administración.")
        }

        return try {
            val usuario = UsuarioMapper.buildAltaUsuario(request.email)
            val passwordTemporal = usuarioService.generarPassword()
            usuario.password = usuarioService.encriptarPassword(passwordTemporal)
            val rolAlumno = rolFactory.build("ALUMNO", usuario)
            usuario.asignarRol(rolAlumno)
            val usuarioGuardado = usuarioService.save(usuario)

            try {
                emailService.enviarEmailAltaUsuario(
                    usuarioGuardado, "Bienvenido/a a Centro SIS", passwordTemporal)
            } catch (e: Exception) {
                e.printStackTrace()
                return ResponseEntity.status(HttpStatus.CREATED).body(
                    CustomResponse(
                        message = "Registro exitoso, pero hubo un problema al enviar el email. " +
                                "Por favor, contactá a la administración para obtener tu contraseña.",
                        data = mapOf("email" to usuario.email, "debeCompletarPerfil" to true)
                    )
                )
            }

            ResponseEntity.status(HttpStatus.CREATED).body(
                CustomResponse(
                    message = "Registro exitoso. Te enviamos un email a ${request.email} " +
                            "con tu contraseña temporal. Revisá tu casilla (incluyendo spam).",
                    data = mapOf("email" to usuario.email, "debeCompletarPerfil" to true)
                )
            )
        } catch (e: ResponseStatusException) {
            throw e
        } catch (e: Exception) {
            e.printStackTrace()
            error(HttpStatus.INTERNAL_SERVER_ERROR,
                "Ocurrió un error inesperado. Por favor, intentá nuevamente.")
        }
    }

    @PostMapping("/reenviar-invitacion/{usuarioId}/{administradorId}")
    fun reenviarInvitacion(
        @PathVariable usuarioId: Long,
        @PathVariable administradorId: Long
    ): ResponseEntity<CustomResponse> {
        administracionService.verificarRol(administradorId)
        val usuario = usuarioService.getById(usuarioId)
        if (!usuario.esPrimerLogin()) {
            return error(HttpStatus.BAD_REQUEST, "El usuario ya realizó el primer login")
        }
        val password = usuarioService.generarPassword()
        usuario.password = usuarioService.encriptarPassword(password)
        val usuarioActualizado = usuarioService.save(usuario)
        try {
            emailService.enviarEmailAltaUsuario(usuario, "Bienvenido a CENTRO SIS", password)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return ok("Invitación reenviada correctamente",
            UsuarioMapper.buildUsuarioResponseDto(usuarioActualizado))
    }

    // ─── Edición ──────────────────────────────────────────────────────────────

    @PutMapping("/registro")
    fun registro(@RequestBody usuarioDto: UsuarioRegistroRequestDto): ResponseEntity<CustomResponse> =
        ok("Registro realizado correctamente",
            UsuarioMapper.buildUsuarioResponseDto(usuarioService.primerLogin(usuarioDto)))

    @PostMapping("/save")
    fun save(@RequestBody usuarioDto: UsuarioRequestDto): ResponseEntity<CustomResponse> {
        val usuario = UsuarioMapper.buildUsuario(usuarioDto)
        usuario.password = usuarioService.findById(usuarioDto.id)!!.password!!
        usuarioService.save(usuario)
        return ok("Usuario editado correctamente", UsuarioMapper.buildUsuarioResponseDto(usuario))
    }

    @PostMapping("/update-password/{id}")
    fun editPassword(
        @PathVariable id: Long,
        @RequestBody usuarioDto: UsuarioCambioPasswordRequestDto
    ): ResponseEntity<CustomResponse> =
        ok("Password actualizado correctamente",
            UsuarioMapper.buildUsuarioResponseDto(usuarioService.updatePassword(usuarioDto, id)))

    @PutMapping("/update-perfil/{id}")
    fun updatePerfil(
        @PathVariable id: Long,
        @RequestBody usuarioDto: UsuarioUpdatePerfilRequestDto
    ): ResponseEntity<CustomResponse> =
        ok("Perfil actualizado correctamente",
            UsuarioMapper.buildUsuarioResponseDto(usuarioService.updatePerfil(usuarioDto, id)))

    // ─── Baja ─────────────────────────────────────────────────────────────────

    @DeleteMapping("delete/{usuarioId}/{eliminadoPorId}")
    fun delete(
        @PathVariable usuarioId: Long,
        @PathVariable eliminadoPorId: Long
    ): ResponseEntity<CustomResponse> {
        usuarioService.darDeBaja(usuarioId, eliminadoPorId)
        return ok("Usuario eliminado correctamente", null)
    }

    // ─── Listados ─────────────────────────────────────────────────────────────

    @GetMapping("/all/{userId}")
    fun getAllUsuarios(@PathVariable userId: Long): ResponseEntity<CustomResponse> =
        ok("Usuarios obtenidos correctamente",
            usuarioService.getAllActivosExceptoDto(userId))

    @GetMapping("/all/alumnos")
    fun getAllAlumnosActivos(): ResponseEntity<CustomResponse> {
        val usuarios = usuarioService.getUsuariosPorRol(RolType.ALUMNO)
            .filter { it.estado == EstadoType.ACTIVO }
            .map { UsuarioMapper.buildUsuarioResponseDto(it) }
        return ok("Alumnos obtenidos correctamente", usuarios)
    }

    @GetMapping("/profesores")
    fun obtenerProfesores(): ResponseEntity<CustomResponse> {
        val profesores = usuarioService.getUsuariosPorRol(RolType.PROFESOR)
            .map { UsuarioMapper.buildProfesoresListaResponseDto(it) }
        return ok("Profesores obtenidos correctamente", profesores)
    }

    // ─── Búsqueda ─────────────────────────────────────────────────────────────

    /**
     * Búsqueda delegada a la DB — ya no carga toda la tabla en memoria.
     */
    @GetMapping("/search-by-rol")
    fun searchByRol(
        @RequestParam rol: String,
        @RequestParam q: String,
        @RequestParam(required = false) cursoId: Long?,
        @RequestParam(defaultValue = "20") limit: Int
    ): ResponseEntity<CustomResponse> {
        if (q.length < 2) {
            return error(HttpStatus.BAD_REQUEST, "La búsqueda debe tener al menos 2 caracteres")
        }
        val rolType = RolType.valueOf(rol)
        var usuarios = usuarioService.searchByRol(q, rolType, limit)

        // Excluir ya inscriptos / ya en el curso — se hace en memoria sobre el resultado pequeño
        if (cursoId != null) {
            val curso = cursoService.getById(cursoId)
            val excluidos = when (rolType) {
                RolType.ALUMNO   -> curso.inscripciones.map { it.alumno.usuario.id }.toSet()
                RolType.PROFESOR -> curso.profesores.map { it.usuario.id }.toSet()
                else             -> emptySet()
            }
            if (excluidos.isNotEmpty()) usuarios = usuarios.filter { it.id !in excluidos }
        }

        return ok("${usuarios.size} usuario(s) encontrado(s)",
            usuarios.map { UsuarioMapper.buildUsuarioResponseDto(it) })
    }

    @GetMapping("/search")
    fun search(
        @RequestParam q: String,
        @RequestParam(defaultValue = "20") limit: Int
    ): ResponseEntity<CustomResponse> {
        if (q.length < 2) {
            return error(HttpStatus.BAD_REQUEST, "La búsqueda debe tener al menos 2 caracteres")
        }
        val usuarios = usuarioService.searchTodos(q, limit)
            .map { UsuarioMapper.buildUsuarioResponseDto(it) }
        return ok("${usuarios.size} usuario(s) encontrado(s)", usuarios)
    }

    // ─── Cursos por usuario ───────────────────────────────────────────────────

    @GetMapping("/cursos-alumno/{idAlumno}")
    fun obtenerCursosActivosDelAlumno(@PathVariable idAlumno: Long): ResponseEntity<CustomResponse> {
        val inscripciones = inscripcionService.obtenerInscripcionesPorAlumno(idAlumno)
        return ok("Cursos obtenidos correctamente",
            inscripciones.map { CursoMapper.buildCursoAlumnoResponseDto(it) })
    }

    @GetMapping("/cursos-profesor/{profesorId}")
    fun obtenerCursosDictadosPorProfesor(@PathVariable profesorId: Long): ResponseEntity<CustomResponse> {
        val cursos = cursoService.obtenerCursosProfesorId(profesorId)
        return ok("Cursos obtenidos correctamente", cursos)
    }

    // ─── Pagos ────────────────────────────────────────────────────────────────

    @GetMapping("/usuarios/{usuarioId}/pagos-recibidos-profesor")
    fun obtenerPagosRecibidosComoProfesor(@PathVariable usuarioId: Long): ResponseEntity<CustomResponse> {
        val profesor = usuarioService.getById(usuarioId).getRolProfesor()
        return ok("Pagos obtenidos correctamente",
            profesor.obtenerPagosRecibidos().map { PagoMapper.buildPagoResponseDto(it) })
    }

    @GetMapping("/usuarios/{usuarioId}/pagos-realizados-profesor")
    fun obtenerPagosRealizadosComoProfesor(@PathVariable usuarioId: Long): ResponseEntity<CustomResponse> {
        val profesor = usuarioService.getById(usuarioId).getRolProfesor()
        return ok("Pagos obtenidos correctamente",
            profesor.obtenerPagosRealizados().map { PagoMapper.buildPagoResponseDto(it) })
    }

    @GetMapping("/pagos-como-alumno/{usuarioId}/{inscripcionId}")
    fun obtenerPagosRealizadosComoAlumno(
        @PathVariable usuarioId: Long,
        @PathVariable inscripcionId: Long
    ): ResponseEntity<CustomResponse> =
        ok("Pagos obtenidos correctamente",
            inscripcionService.obtenerPagosAlumno(usuarioId, inscripcionId)
                .map { PagoMapper.buildPagoResponseDto(it) })

    @GetMapping("/pagos-como-alumno/{usuarioId}")
    fun obtenerTodosLosPagosRealizadosComoAlumno(
        @PathVariable usuarioId: Long
    ): ResponseEntity<CustomResponse> =
        ok("Pagos obtenidos correctamente",
            inscripcionService.obtenerTodosLosPagosAlumno(usuarioId)
                .map { PagoMapper.buildPagoResponseDto(it) })

    // ─── Detalle completo ─────────────────────────────────────────────────────

    @GetMapping("/detalle/{usuarioId}")
    fun getUsuarioById(@PathVariable usuarioId: Long): ResponseEntity<CustomResponse> {
        val usuario = usuarioService.findById(usuarioId)
            ?: throw NoSuchElementException("Usuario no encontrado")

        val cursosDictados = mutableListOf<CursoResponseDto>()
        val cursosInscriptos = mutableListOf<CursoAlumnoResponseDto>()

        if (usuario.tieneRol(RolType.PROFESOR)) {
            cursoService.obtenerCursosProfesorId(usuarioId)
        }
        if (usuario.tieneRol(RolType.ALUMNO)) {
            inscripcionService.obtenerInscripcionesPorAlumno(usuarioId)
                .mapTo(cursosInscriptos) { CursoMapper.buildCursoAlumnoResponseDto(it) }
        }

        return ok("Usuario obtenido correctamente",
            UsuarioMapper.buildUsuarioDetailDto(usuario, cursosInscriptos, cursosDictados))
    }

    // ─── Roles ────────────────────────────────────────────────────────────────

    @PostMapping("/asignar-rol/{usuarioId}/{rolType}")
    fun asignarRol(
        @PathVariable usuarioId: Long,
        @PathVariable rolType: String
    ): ResponseEntity<CustomResponse> {
        val usuario = usuarioService.getById(usuarioId)
        usuario.asignarRol(rolFactory.build(rolType, usuario))
        usuarioService.save(usuario)
        return ok("Rol asignado correctamente", UsuarioMapper.buildUsuarioResponseDto(usuario))
    }

    @DeleteMapping("/{usuarioId}/remover-rol/{rol}")
    fun removerRol(
        @PathVariable usuarioId: Long,
        @PathVariable rol: String
    ): ResponseEntity<CustomResponse> {
        val usuario = usuarioService.getById(usuarioId)
        usuario.quitarRol(RolType.valueOf(rol))
        usuarioService.save(usuario)
        return ok("Rol removido correctamente", UsuarioMapper.buildUsuarioResponseDto(usuario))
    }

    // ─── Recuperar contraseña ─────────────────────────────────────────────────

    @PostMapping("/recuperar-password")
    fun recuperarPassword(@RequestBody request: RecuperarPasswordRequestDto): ResponseEntity<CustomResponse> {
        usuarioService.solicitarRecuperarPassword(request.email)
        return ok("Si el email está registrado, recibirás las instrucciones para restablecer tu contraseña.", null)
    }

    // ─── Helpers privados ─────────────────────────────────────────────────────

    private fun ok(message: String, data: Any?) =
        ResponseEntity.ok(CustomResponse(message = message, data = data))

    private fun error(status: HttpStatus, message: String) =
        ResponseEntity.status(status).body(CustomResponse(message = message, data = null))
}