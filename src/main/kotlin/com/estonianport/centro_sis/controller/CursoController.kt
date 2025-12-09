package com.estonianport.centro_sis.controller

import com.estonianport.centro_sis.dto.request.CursoRequestDto
import com.estonianport.centro_sis.dto.response.CustomResponse
import com.estonianport.centro_sis.mapper.CursoMapper
import com.estonianport.centro_sis.model.RolFactory
import com.estonianport.centro_sis.service.BeneficioService
import com.estonianport.centro_sis.service.CursoService
import com.estonianport.centro_sis.service.PagoService
import com.estonianport.centro_sis.service.RolService
import com.estonianport.centro_sis.service.UsuarioService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/curso")
@CrossOrigin("*")
class CursoController(
    private val cursoService: CursoService,
    private val usuarioService: UsuarioService,
    private val rolService: RolService,
    private val rolFactory: RolFactory,
    private val pagoService: PagoService
) {

    @GetMapping("/{id}")
    fun get(@PathVariable id: Long): ResponseEntity<CustomResponse> {
        val curso = cursoService.getById(id)
        val profesores = rolService.getProfesorByCursoId(id)

        return ResponseEntity.status(200).body(
            CustomResponse(
                message = "Curso obtenido correctamente",
                data = CursoMapper.buildCursoResponseDto(curso, profesores)
            )
        )
    }

    @GetMapping("/getAllByAlumnoId/{idAlumno}")
    fun getAllByAlumnoId(@PathVariable idAlumno: Long): ResponseEntity<CustomResponse> {
        val listaCurso = cursoService.getAllCursosByAlumnoId(idAlumno)
        val usuario = usuarioService.getById(idAlumno)
        val beneficios = usuario.beneficios
        val rolAlumno = usuario.getRolAlumno()

        val listaCursoDto = listaCurso.map {
            CursoMapper.buildCursoAlumnoResponseDto(
                it,
                rolService.getProfesorByCursoId(it.id),
                beneficios,
                rolAlumno.getInscripcionPorCurso(it).estadoPago
            )
        }

        return ResponseEntity.status(200).body(
            CustomResponse(
                message = "Curso obtenido correctamente",
                data = listaCursoDto
            )
        )
    }

    @GetMapping("/getAllByProfesorId/{idProfesor}")
    fun getAllByProfesorId(@PathVariable idProfesor: Long): ResponseEntity<CustomResponse> {
        val listaCurso = rolService.getCursosByProfesorId(idProfesor)

        val listaCursoDto = listaCurso.map {
            CursoMapper.buildCursoProfesorResponseDto(
                it,
                cursoService.cantAlumnosInscritos(it.id)
            )
        }

        return ResponseEntity.status(200).body(
            CustomResponse(
                message = "Curso obtenido correctamente",
                data = listaCursoDto
            )
        )
    }

    @PostMapping("/alta")
    fun alta(@RequestBody cursoRequestDto: CursoRequestDto): ResponseEntity<CustomResponse> {
        val nuevoCurso = CursoMapper.buildCurso(cursoRequestDto)
        val cursoAgregado = cursoService.alta(nuevoCurso)

        //Una vez que el curso se creo correctamente, osea no ya esta persistido,
        //le asigno rol de profesor a los usuarios seleccionados
        cursoRequestDto.profesoresId.map { usuarioId ->
            val usuario = usuarioService.getById(usuarioId)
            val rol = rolFactory.build("PROFESOR", usuario)
            usuario.asignarRol(rol)
            usuarioService.save(usuario)
        }

        return ResponseEntity.status(201).body(
            CustomResponse(
                message = "Curso creado correctamente",
                data = cursoAgregado
            )
        )
    }

    @GetMapping("/all")
    fun getAll(): ResponseEntity<CustomResponse> {
        val cursos = cursoService.getAllCursos()
        return ResponseEntity.status(200).body(
            CustomResponse(
                message = "Cursos obtenidos correctamente",
                data = cursos.map { CursoMapper.buildCursoResponseDto(it, rolService.getProfesorByCursoId(it.id)) }
            )
        )
    }
}