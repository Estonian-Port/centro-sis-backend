package com.estonianport.centro_sis.controller

import com.estonianport.centro_sis.dto.request.CursoAlquilerRequestDto
import com.estonianport.centro_sis.dto.request.CursoComisionRequestDto
import com.estonianport.centro_sis.dto.response.CustomResponse
import com.estonianport.centro_sis.mapper.CursoMapper
import com.estonianport.centro_sis.mapper.TipoPagoMapper
import com.estonianport.centro_sis.model.enums.EstadoType
import com.estonianport.centro_sis.service.CursoService
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
) {

    //Obtener un curso por id
    @GetMapping("/{id}")
    fun get(@PathVariable id: Long): ResponseEntity<CustomResponse> {
        val curso = cursoService.getById(id)

        return ResponseEntity.status(200).body(
            CustomResponse(
                message = "Curso obtenido correctamente",
                data = CursoMapper.buildCursoResponseDto(
                    curso,
                    cursoService.cantAlumnosInscriptos(curso.id))
            )
        )
    }

    //Obtener todos los cursos activos
    @GetMapping("/activos")
    fun get(): ResponseEntity<CustomResponse> {
        val cursos = cursoService.getAllCursos()

        return ResponseEntity.status(200).body(
            CustomResponse(
                message = "Curso obtenido correctamente",
                data = cursos.map {
                    CursoMapper.buildCursoResponseDto(
                        it,
                        cursoService.cantAlumnosInscriptos(it.id)
                    )
                }
            )
        )
    }

    //Crear un nuevo curso alquiler
    @PostMapping("/alta-alquiler")
    fun altaAlquiler(@RequestBody cursoRequestDto: CursoAlquilerRequestDto): ResponseEntity<CustomResponse> {
        val profesores = cursoRequestDto.profesoresId
            .map { usuarioService.getById(it).getRolProfesor() }
            .toMutableSet()

        val nuevoCursoAlquiler = CursoMapper.buildCursoAlquiler(cursoRequestDto, profesores)
        val cursoAgregado = cursoService.alta(nuevoCursoAlquiler)

        return ResponseEntity.status(201).body(
            CustomResponse(
                message = "Curso creado correctamente",
                data = cursoAgregado
            )
        )
    }

    //Crear un nuevo curso comision
    @PostMapping("/alta-comision")
    fun altaComision(@RequestBody cursoRequestDto: CursoComisionRequestDto): ResponseEntity<CustomResponse> {
        val profesores = cursoRequestDto.profesoresId
            .map { usuarioService.getById(it).getRolProfesor() }
            .toMutableSet()

        val nuevoCursoComision = CursoMapper.buildCursoComision(cursoRequestDto, profesores)
        nuevoCursoComision.activo = EstadoType.ACTIVO

        val cursoAgregado = cursoService.alta(nuevoCursoComision)

        return ResponseEntity.status(201).body(
            CustomResponse(
                message = "Curso creado correctamente",
                data = cursoAgregado
            )
        )
    }

    /*
        REVISAR ESTOS DOS ENDPOINTS DE ABAJO

        //Dar de baja un curso
        @PostMapping("/baja/{id}")
        fun baja(@PathVariable id: Long): ResponseEntity<CustomResponse> {
            cursoService.delete(id)
            return ResponseEntity.status(200).body(
                CustomResponse(
                    message = "Curso dado de baja correctamente",
                    data = null
                )
            )
        }

        //Editar un curso
        @PostMapping("/editar/{id}")
        fun editar(@PathVariable id: Long, @RequestBody cursoRequestDto: CursoRequestDto): ResponseEntity<CustomResponse> {
            val cursoExistente = cursoService.getById(id)
            val cursoEditado = CursoMapper.buildCurso(cursoRequestDto, cursoExistente)
            cursoService.save(cursoEditado)
            return ResponseEntity.status(200).body(
                CustomResponse(
                    message = "Curso editado correctamente",
                    data = cursoEditado
                )
            )
        }

     */
}