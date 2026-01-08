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
    @DeleteMapping("delete/{usuarioId}/{administradorId}")
    fun delete(@PathVariable usuarioId: Long, @PathVariable administradorId: Long): ResponseEntity<CustomResponse> {
        administracionService.verificarRol(administradorId)
        usuarioService.darDeBaja(usuarioId)
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
        usuarioService.verificarEmailNoExiste(usuarioDto.email)
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

    // Registro de usuario (primer login)
    @PutMapping("/registro")
    fun registro(@RequestBody usuarioDto: UsuarioRegistroRequestDto): ResponseEntity<CustomResponse> {
        usuarioService.primerLogin(usuarioDto)
        val usuario = usuarioService.findById(usuarioDto.id)
        return ResponseEntity.status(200).body(
            CustomResponse(
                message = "Registro realizado correctamente",
                data = UsuarioMapper.buildUsuarioResponseDto(usuario!!)
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
    @PostMapping("/update-password")
    fun editPassword(@RequestBody usuarioDto: UsuarioCambioPasswordRequestDto): ResponseEntity<CustomResponse> {
        return ResponseEntity.status(200).body(
            CustomResponse(
                message = "Password actualizado correctamente",
                data = UsuarioMapper.buildUsuarioResponseDto(usuarioService.updatePassword(usuarioDto))
            )
        )
    }

    // Editar perfil de usuario (nombre, apellido, telefono, etc) menos la password
    @PutMapping("/update-perfil")
    fun updatePerfil(@RequestBody usuarioDto: UsuarioRequestDto): ResponseEntity<CustomResponse> {
        val usuario = UsuarioMapper.buildUsuario(usuarioDto)
        return ResponseEntity.status(200).body(
            CustomResponse(
                message = "Perfil actualizado correctamente",
                data = UsuarioMapper.buildUsuarioResponseDto(usuarioService.updatePerfil(usuario))
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
                        curso.inscripciones.map { UsuarioMapper.buildAlumno(it) }
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
                //ACA TAMBIEN TENGO QUE PONER LOS PAGOS RECIBIDOS PARA LOS CURSOS ALQUILER QUE TENGA A CARGO
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
                        curso.inscripciones.map { UsuarioMapper.buildAlumno(it) }
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

}