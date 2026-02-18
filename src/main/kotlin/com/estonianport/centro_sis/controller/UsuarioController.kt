package com.estonianport.centro_sis.controller

import com.estonianport.centro_sis.common.emailService.EmailService
import com.estonianport.centro_sis.dto.response.CustomResponse
import com.estonianport.centro_sis.mapper.UsuarioMapper
import com.estonianport.centro_sis.service.AdministracionService
import com.estonianport.centro_sis.service.UsuarioService
import com.estonianport.centro_sis.dto.request.UsuarioAltaRequestDto
import com.estonianport.centro_sis.dto.request.UsuarioCambioPasswordRequestDto
import com.estonianport.centro_sis.dto.request.UsuarioRegistroRequestDto
import com.estonianport.centro_sis.dto.request.UsuarioRequestDto
import com.estonianport.centro_sis.dto.request.UsuarioUpdatePerfilRequestDto
import com.estonianport.centro_sis.dto.response.CursoAlumnoResponseDto
import com.estonianport.centro_sis.dto.response.CursoResponseDto
import com.estonianport.centro_sis.mapper.CursoMapper
import com.estonianport.centro_sis.mapper.PagoMapper
import com.estonianport.centro_sis.model.RolFactory
import com.estonianport.centro_sis.model.enums.RolType
import com.estonianport.centro_sis.service.CursoService
import com.estonianport.centro_sis.service.InscripcionService
import org.springframework.beans.factory.annotation.Autowired
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
import java.security.Principal
import java.time.LocalDate

@RestController
@RequestMapping("/usuario")
@CrossOrigin("*")
class UsuarioController(
    private val rolFactory: RolFactory,
) {

    @Autowired
    private lateinit var inscripcionService: InscripcionService

    @Autowired
    private lateinit var emailService: EmailService

    @Autowired
    lateinit var administracionService: AdministracionService

    @Autowired
    lateinit var usuarioService: UsuarioService

    @Autowired
    lateinit var cursoService: CursoService

    //Obtener usuario logueado
    @GetMapping("/me")
    fun getCurrent(principal: Principal): ResponseEntity<CustomResponse> {
        val email = principal.name
        val usuario = usuarioService.getUsuarioByEmail(email)

        return ResponseEntity.status(200).body(
            CustomResponse(
                message = "Usuario obtenido correctamente",
                data = UsuarioMapper.buildUsuarioResponseDto(usuario)
            )
        )
    }

    //Baja de usuario
    @DeleteMapping("delete/{usuarioId}/{eliminadoPorId}")
    fun delete(@PathVariable usuarioId: Long, @PathVariable eliminadoPorId: Long): ResponseEntity<CustomResponse> {
        usuarioService.darDeBaja(usuarioId, eliminadoPorId)
        return ResponseEntity.status(200).body(
            CustomResponse(
                message = "Usuario eliminado correctamente",
                data = null
            )
        )
    }

    //Alta de usuario
    @PostMapping("/altaUsuario")
    fun altaUsuario(@RequestBody usuarioDto: UsuarioAltaRequestDto): ResponseEntity<CustomResponse> {
        usuarioService.verificarEmailNoExistente(usuarioDto.email)
        val usuario = UsuarioMapper.buildAltaUsuario(usuarioDto)

        val password = usuarioService.generarPassword()
        usuario.password = usuarioService.encriptarPassword(password)
        usuario.fechaAlta = LocalDate.now()
        usuarioDto.roles.forEach {
            val rol = rolFactory.build(it, usuario)
            usuario.asignarRol(rol)
        }

        usuarioService.save(usuario)

        try {
            emailService.enviarEmailAltaUsuario(usuario, "Bienvenido a CENTRO SIS", password)
        } catch (e: Exception) {
            e.printStackTrace()
            // TODO enviar notificacion de fallo al enviar el mail
        }

        return ResponseEntity.status(200).body(
            CustomResponse(
                message = "Usuario creado correctamente",
                data = UsuarioMapper.buildUsuarioResponseDto(usuario)
            )
        )
    }

    //Endpoint para reenviar invitacion por mail a un usuario que aun no hizo el primer login
    @PostMapping("/reenviar-invitacion/{usuarioId}/{administradorId}")
    fun reenviarInvitacion(
        @PathVariable usuarioId: Long,
        @PathVariable administradorId: Long
    ): ResponseEntity<CustomResponse> {
        administracionService.verificarRol(administradorId)
        val usuario = usuarioService.getById(usuarioId)
        if (!usuario.esPrimerLogin()) {
            return ResponseEntity.status(400).body(
                CustomResponse(
                    message = "El usuario ya realizó el primer login",
                    data = null
                )
            )
        }
        val password = usuarioService.generarPassword()
        usuario.password = usuarioService.encriptarPassword(password)
        val usuarioActualizado = usuarioService.save(usuario)
        try {
            emailService.enviarEmailAltaUsuario(usuario, "Bienvenido a CENTRO SIS", password)
        } catch (e: Exception) {
            e.printStackTrace()
            // TODO enviar notificacion de fallo al enviar el mail
        }

        return ResponseEntity.status(200).body(
            CustomResponse(
                message = "Usuario creado correctamente",
                data = UsuarioMapper.buildUsuarioResponseDto(usuarioActualizado)
            )
        )
    }

    // Registro de usuario (primer login)
    @PutMapping("/registro")
    fun registro(@RequestBody usuarioDto: UsuarioRegistroRequestDto): ResponseEntity<CustomResponse> {
        val usuario = usuarioService.primerLogin(usuarioDto)
        return ResponseEntity.status(200).body(
            CustomResponse(
                message = "Registro realizado correctamente",
                data = UsuarioMapper.buildUsuarioResponseDto(usuario)
            )
        )
    }

    // No se para que se usa este endpoint, pero lo dejo por las dudas
    @PostMapping("/save")
    fun save(@RequestBody usuarioDto: UsuarioRequestDto): ResponseEntity<CustomResponse> {
        val usuario = UsuarioMapper.buildUsuario(usuarioDto)

        // Traemos la password del back para que no viaje por temas de seguridad al editar un usuario
        // Para editar password usamos el endpoint especifico /editPassword
        usuario.password = usuarioService.findById(usuarioDto.id)!!.password!!


        usuarioService.save(usuario)

        return ResponseEntity.status(200).body(
            CustomResponse(
                message = "Usuario editado correctamente",
                data = UsuarioMapper.buildUsuarioResponseDto(usuario)
            )
        )
    }

    // Busca el usuario por id y encriptar la nueva password
    @PostMapping("/update-password/{id}")
    fun editPassword(
        @PathVariable id: Long,
        @RequestBody usuarioDto: UsuarioCambioPasswordRequestDto
    ): ResponseEntity<CustomResponse> {
        return ResponseEntity.status(200).body(
            CustomResponse(
                message = "Password actualizado correctamente",
                data = UsuarioMapper.buildUsuarioResponseDto(usuarioService.updatePassword(usuarioDto, id))
            )
        )
    }

    // Editar perfil de usuario (nombre, apellido, telefono, etc) menos la password
    @PutMapping("/update-perfil/{id}")
    fun updatePerfil(
        @PathVariable id: Long,
        @RequestBody usuarioDto: UsuarioUpdatePerfilRequestDto
    ): ResponseEntity<CustomResponse> {
        return ResponseEntity.status(200).body(
            CustomResponse(
                message = "Perfil actualizado correctamente",
                data = UsuarioMapper.buildUsuarioResponseDto(usuarioService.updatePerfil(usuarioDto, id))
            )
        )
    }

    //Endpoint para obtener todos los usuarios menos el que realiza la peticion, se usa en la vista
    //de gestion de usuarios en la vista del administrador
    @GetMapping("/all/{userId}")
    fun getAllUsuarios(@PathVariable userId: Long): ResponseEntity<CustomResponse> {
        val usuarios = usuarioService.getAllUsuarios()
        val usuariosDto = usuarios.filter { it.id != userId }.map { UsuarioMapper.buildUsuarioResponseDto(it) }
        return ResponseEntity.status(200).body(
            CustomResponse(
                message = "Usuarios obtenidos correctamente",
                data = usuariosDto
            )
        )
    }

    //Endpoint para obtener todos los usuarios con rol Alumno que este activos
    @GetMapping("/all/alumnos")
    fun getAllAlumnosActivos(): ResponseEntity<CustomResponse> {
        val usuarios = usuarioService.getUsuariosPorRol(RolType.ALUMNO).filter { it.estado.name == "ACTIVO" }
        val usuariosDto = usuarios.map { UsuarioMapper.buildUsuarioResponseDto(it) }
        return ResponseEntity.status(200).body(
            CustomResponse(
                message = "Alumnos obtenidos correctamente",
                data = usuariosDto
            )
        )
    }

    // Enpoint de busqueda de alumnos por nombre, apellido, mail o dni. Se usa en la inscripcion a cursos
    @GetMapping("/search-by-rol")
    fun searchAlumnos(
        @RequestParam rol: String,
        @RequestParam q: String,
        @RequestParam(required = false) cursoId: Long?,
        @RequestParam(defaultValue = "20") limit: Int
    ): ResponseEntity<CustomResponse> {
        if (q.length < 2) {
            return ResponseEntity.status(400).body(
                CustomResponse(
                    message = "La búsqueda debe tener al menos 2 caracteres",
                    data = emptyList<Any>()
                )
            )
        }

        val queryLower = q.lowercase()
        var usuarios = usuarioService.getUsuariosPorRol(RolType.valueOf(rol))
            .filter { it.estado.name == "ACTIVO" || it.estado.name == "INACTIVO" }
            .filter { usuario ->
                usuario.nombre.lowercase().contains(queryLower) ||
                        usuario.apellido.lowercase().contains(queryLower) || usuario.dni.lowercase()
                    .contains(queryLower) || usuario.email.lowercase()
                    .contains(queryLower)
            }

        // Excluir alumnos ya inscriptos en el curso
        if (rol == "ALUMNO" && cursoId != null) {
            val curso = cursoService.getById(cursoId)
            val alumnosInscriptosIds = curso.inscripciones.map { it.alumno.usuario.id }.toSet()
            usuarios = usuarios.filter { it.id !in alumnosInscriptosIds }
        }

        //Excluir profesores que dictan el curso
        if (rol == "PROFESOR" && cursoId != null) {
            val curso = cursoService.getById(cursoId)
            val profesoresIds = curso.profesores.map { it.usuario.id }.toSet()
            usuarios = usuarios.filter { it.id !in profesoresIds }
        }

        usuarios = usuarios.take(limit.coerceAtMost(50)) // Máximo 50

        val usuariosDto = usuarios.map { UsuarioMapper.buildUsuarioResponseDto(it) }
        return ResponseEntity.status(200).body(
            CustomResponse(
                message = "${usuariosDto.size} usuario(s) encontrado(s)",
                data = usuariosDto
            )
        )
    }

    // Enpoint de busqueda de usuarios por nombre, apellido, mail o dni.
    @GetMapping("/search")
    fun search(
        @RequestParam q: String,
        @RequestParam(defaultValue = "20") limit: Int
    ): ResponseEntity<CustomResponse> {
        if (q.length < 2) {
            return ResponseEntity.status(400).body(
                CustomResponse(
                    message = "La búsqueda debe tener al menos 2 caracteres",
                    data = emptyList<Any>()
                )
            )
        }

        val queryLower = q.lowercase()
        var usuarios = usuarioService.getAllUsuarios()
            .filter { it.estado.name == "ACTIVO" || it.estado.name == "INACTIVO" }
            .filter { usuario ->
                usuario.nombre.lowercase().contains(queryLower) ||
                        usuario.apellido.lowercase().contains(queryLower) || usuario.dni.lowercase()
                    .contains(queryLower) || usuario.email.lowercase()
                    .contains(queryLower)
            }

        usuarios = usuarios.take(limit.coerceAtMost(50)) // Máximo 50

        val usuariosDto = usuarios.map { UsuarioMapper.buildUsuarioResponseDto(it) }
        return ResponseEntity.status(200).body(
            CustomResponse(
                message = "${usuariosDto.size} usuario(s) encontrado(s)",
                data = usuariosDto
            )
        )
    }


    // Obtener todas los cursos de un alumno
    @GetMapping("/cursos-alumno/{idAlumno}")
    fun obtenerCursosActivosDelAlumno(@PathVariable idAlumno: Long): ResponseEntity<CustomResponse> {
        val inscripciones = usuarioService.obtenerInscripcionesPorAlumno(idAlumno)

        return ResponseEntity.status(200).body(
            CustomResponse(
                message = "Cursos obtenidos correctamente",
                data = inscripciones.map {
                    CursoMapper.buildCursoAlumnoResponseDto(it)
                }
            )
        )
    }

    // Obtener todos los cursos dictados por un profesor
    @GetMapping("/cursos-profesor/{profesorId}")
    fun obtenerCursosDictadosPorProfesor(@PathVariable profesorId: Long): ResponseEntity<CustomResponse> {
        val listaCursosProfesor = usuarioService.obtenerCursosProfesor(profesorId)

        return ResponseEntity.status(200).body(
            CustomResponse(
                message = "Curso obtenido correctamente",
                data = listaCursosProfesor.map { curso ->
                    CursoMapper.buildCursoResponseDto(
                        curso,
                        curso.inscripciones.map { UsuarioMapper.buildAlumno(it.alumno.usuario) }
                    )
                }
            )
        )
    }

    // Obtener pagos recibidos por un profesor
    @GetMapping("/usuarios/{usuarioId}/pagos-recibidos-profesor")
    fun obtenerPagosRecibidosComoProfesor(@PathVariable usuarioId: Long): ResponseEntity<CustomResponse> {
        val usuario = usuarioService.getById(usuarioId)
        val profesor = usuario.getRolProfesor()

        return ResponseEntity.status(200).body(
            CustomResponse(
                message = "Pagos obtenidos correctamente",
                data = profesor.obtenerPagosRecibidos().map { PagoMapper.buildPagoResponseDto(it) }
            )
        )
    }

    // Obtener pagos realizados por un profesor
    @GetMapping("/usuarios/{usuarioId}/pagos-realizados-profesor")
    fun obtenerPagosRealizadosComoProfesor(@PathVariable usuarioId: Long): ResponseEntity<CustomResponse> {
        val usuario = usuarioService.getById(usuarioId)
        val profesor = usuario.getRolProfesor()

        return ResponseEntity.status(200).body(
            CustomResponse(
                message = "Pagos obtenidos correctamente",
                data = profesor.obtenerPagosRealizados().map { PagoMapper.buildPagoResponseDto(it) }
            )
        )
    }

    // Obtener pagos realizados por un alumno en un curso específico
    @GetMapping("/pagos-como-alumno/{usuarioId}/{inscripcionId}")
    fun obtenerPagosRealizadosComoAlumno(
        @PathVariable usuarioId: Long,
        @PathVariable inscripcionId: Long
    ): ResponseEntity<CustomResponse> {
        return ResponseEntity.status(200).body(
            CustomResponse(
                message = "Pagos obtenidos correctamente",
                data = inscripcionService.obtenerPagosAlumno(usuarioId, inscripcionId)
                    .map { PagoMapper.buildPagoResponseDto(it) }
            )
        )
    }

    // Obtener todos los pagos realizados por un alumno
    @GetMapping("/pagos-como-alumno/{usuarioId}")
    fun obtenerTodosLosPagosRealizadosComoAlumno(@PathVariable usuarioId: Long): ResponseEntity<CustomResponse> {
        return ResponseEntity.status(200).body(
            CustomResponse(
                message = "Pagos obtenidos correctamente",
                data = inscripcionService.obtenerTodosLosPagosAlumno(usuarioId)
                    .map { PagoMapper.buildPagoResponseDto(it) }
            )
        )
    }

    // Obtener la lista de profesores para mostrar a la hora de dar de alta un curso
    @GetMapping("/profesores")
    fun obtenerProfesores(): ResponseEntity<CustomResponse> {
        val profesores = usuarioService.getUsuariosPorRol(RolType.PROFESOR)
        return ResponseEntity.status(200).body(
            CustomResponse(
                message = "Profesores obtenidos correctamente",
                data = profesores.map { UsuarioMapper.buildProfesoresListaResponseDto(it) }
            )
        )
    }

    //Obtener informacion completa de un usuario por su ID
    @GetMapping("/detalle/{usuarioId}")
    fun getUsuarioById(@PathVariable usuarioId: Long): ResponseEntity<CustomResponse> {
        val usuario = usuarioService.findById(usuarioId) ?: throw NoSuchElementException("Usuario no encontrado")
        val cursosDictados = mutableListOf<CursoResponseDto>()
        val cursosInscriptos = mutableListOf<CursoAlumnoResponseDto>()
        if (usuario.tieneRol(RolType.PROFESOR)) {
            val cursos = usuarioService.obtenerCursosProfesor(usuarioId)
                .map { curso ->
                    CursoMapper.buildCursoResponseDto(
                        curso,
                        curso.inscripciones.map { UsuarioMapper.buildAlumno(it.alumno.usuario) }
                    )
                }
            cursosDictados.addAll(cursos)
        }
        if (usuario.tieneRol(RolType.ALUMNO)) {
            val inscripciones = usuarioService.obtenerInscripcionesPorAlumno(usuarioId)
            val cursos = inscripciones.map {
                CursoMapper.buildCursoAlumnoResponseDto(it)
            }
            cursosInscriptos.addAll(cursos)
        }
        return ResponseEntity.status(200).body(
            CustomResponse(
                message = "Usuario obtenido correctamente",
                data = UsuarioMapper.buildUsuarioDetailDto(usuario, cursosInscriptos, cursosDictados)
            )
        )
    }

    //Endpoint para asignar rol a un usuario
    @PostMapping("/asignar-rol/{usuarioId}/{rolType}")
    fun asignarRol(
        @PathVariable usuarioId: Long,
        @PathVariable rolType: String
    ): ResponseEntity<CustomResponse> {
        val usuario = usuarioService.getById(usuarioId)
        val rol = rolFactory.build(rolType, usuario)
        usuario.asignarRol(rol)
        usuarioService.save(usuario)
        return ResponseEntity.status(200).body(
            CustomResponse(
                message = "Rol asignado correctamente",
                data = UsuarioMapper.buildUsuarioResponseDto(usuario)
            )
        )
    }

    //Endpoint para remover rol a un usuario
    @DeleteMapping("/{usuarioId}/remover-rol/{rol}")
    fun removerRol(
        @PathVariable usuarioId: Long,
        @PathVariable rol: String
    ): ResponseEntity<CustomResponse> {
        val usuario = usuarioService.getById(usuarioId)
        usuario.quitarRol(RolType.valueOf(rol))
        usuarioService.save(usuario)
        return ResponseEntity.status(200).body(
            CustomResponse(
                message = "Rol asignado correctamente",
                data = UsuarioMapper.buildUsuarioResponseDto(usuario)
            )
        )
    }

}