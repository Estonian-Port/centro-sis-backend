package com.estonianport.centro_sis.service

import com.estonianport.centro_sis.common.AppTime
import com.estonianport.centro_sis.common.GenericServiceImpl
import com.estonianport.centro_sis.common.errors.NotFoundException
import com.estonianport.centro_sis.dto.request.CursoAlquilerAdminRequestDto
import com.estonianport.centro_sis.dto.request.CursoComisionRequestDto
import com.estonianport.centro_sis.dto.response.CursoResponseDto
import com.estonianport.centro_sis.dto.response.ParteAsistenciaResponseDto
import com.estonianport.centro_sis.mapper.CursoMapper
import com.estonianport.centro_sis.mapper.ParteAsistenciaMapper
import com.estonianport.centro_sis.model.Curso
import com.estonianport.centro_sis.model.Horario
import com.estonianport.centro_sis.model.ParteAsistencia
import com.estonianport.centro_sis.model.RolProfesor
import com.estonianport.centro_sis.model.enums.EstadoType
import com.estonianport.centro_sis.model.enums.TipoPago
import com.estonianport.centro_sis.repository.CursoRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.cache.annotation.Caching
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDate

@Service
@Transactional(readOnly = true)
class CursoService : GenericServiceImpl<Curso, Long>() {

    @Autowired private lateinit var usuarioService: UsuarioService
    @Autowired lateinit var cursoRepository: CursoRepository

    override val dao: CursoRepository
        get() = cursoRepository

    fun countCursos(): Long = cursoRepository.countByFechaBajaIsNull()

    // ─── LECTURAS ─────────────────────────────────────────────────────────────

    /**
     * Lista principal: devuelve DTOs directamente para que el caché no guarde
     * entidades JPA con lazy collections → evita el Document nesting depth (1001).
     */
    @Cacheable(value = ["cursos:lista"], key = "'todos'")
    fun getAllCursosResponse(): List<CursoResponseDto> {
        val cursos = cursoRepository.findAllOrdenadosConDetalles().distinct()
        if (cursos.isEmpty()) return emptyList()
        // Segunda pasada: inscripciones (evita producto cartesiano en un solo JOIN)
        cursoRepository.findCursosConInscripcionesByIdsIn(cursos.map { it.id })
        return cursos.map { CursoMapper.buildCursoResponseDto(it) }
    }

    /**
     * Carga la entidad para operaciones de escritura o uso interno.
     * NO cachea la entidad — solo el DTO resultante se puede cachear externamente.
     */
    fun getById(id: Long): Curso {
        cursoRepository.findByIdConDetalles(id)
            .orElseThrow { NotFoundException("No se encontró el curso con ID: $id") }
        return cursoRepository.findInscripcionesByCursoId(id)
            .orElseThrow { NotFoundException("No se encontró el curso con ID: $id") }
    }

    /**
     * Versión cacheada que devuelve DTO para el endpoint GET /curso/{id}.
     */
    @Cacheable(value = ["cursos:detalle"], key = "#id")
    fun getByIdDto(id: Long): CursoResponseDto =
        CursoMapper.buildCursoResponseDto(getById(id))

    @Cacheable(value = ["cursos:profesor"], key = "#idProfe")
    fun obtenerCursosProfesorId(idProfe: Long): List<CursoResponseDto> {
        val cursosEntidad = cursoRepository.findCursosActivosPorProfesorId(idProfe)
        return cursosEntidad.map { CursoMapper.buildCursoResponseDto(it) }
    }
    @Cacheable(value = ["cursos:asistencia"], key = "#cursoId")
    fun getPartesAsistencia(cursoId: Long): List<ParteAsistenciaResponseDto> {
        val partes = cursoRepository.findPartesAsistenciaByCursoId(cursoId)
        return partes.map { ParteAsistenciaMapper.buildParteAsistenciaResponseDto(it) }
    }

    // ─── ESCRITURAS ───────────────────────────────────────────────────────────

    @Transactional
    @Caching(evict = [
        CacheEvict(value = ["cursos:lista"], key = "'todos'"),
    ])
    fun alta(nuevoCurso: Curso): Curso = cursoRepository.save(nuevoCurso)

    @Transactional
    @Caching(evict = [
        CacheEvict(value = ["cursos:lista"],    key = "'todos'"),
        CacheEvict(value = ["cursos:profesor"], allEntries = true),
    ])
    fun crearCursoAlquiler(cursoRequestDto: CursoAlquilerAdminRequestDto): Curso {
        val profesores = cursoRequestDto.profesoresId
            .map { usuarioService.getById(it).getRolProfesor() }
            .toMutableSet()
        val nuevoCurso = CursoMapper.buildCursoAlquiler(cursoRequestDto, profesores)
        val cursoAgregado = alta(nuevoCurso)
        usuarioService.actualizarEstadoProfesor(profesores)
        return cursoAgregado
    }

    @Transactional
    @Caching(evict = [
        CacheEvict(value = ["cursos:lista"],    key = "'todos'"),
        CacheEvict(value = ["cursos:profesor"], allEntries = true),
    ])
    fun crearCursoComision(cursoRequestDto: CursoComisionRequestDto): Curso {
        val profesores = cursoRequestDto.profesoresId
            .map { usuarioService.getById(it).getRolProfesor() }
            .toMutableSet()
        val nuevoCurso = CursoMapper.buildCursoComision(cursoRequestDto, profesores)
        nuevoCurso.estadoAlta = EstadoType.ACTIVO
        val cursoAgregado = alta(nuevoCurso)
        usuarioService.actualizarEstadoProfesor(profesores)
        return cursoAgregado
    }

    @Transactional
    @Caching(evict = [
        CacheEvict(value = ["cursos:lista"],   key = "'todos'"),
        CacheEvict(value = ["cursos:detalle"], key = "#cursoId"),
    ])
    fun actualizarNombreCurso(cursoId: Long, nuevoNombre: String): CursoResponseDto {
        val updated = cursoRepository.updateNombre(cursoId, nuevoNombre)
        if (updated == 0) throw NotFoundException("No se encontró el curso con ID: $cursoId")
        return getByIdDto(cursoId)
    }

    @Transactional
    @Caching(evict = [
        CacheEvict(value = ["cursos:lista"],    key = "'todos'"),
        CacheEvict(value = ["cursos:detalle"],  allEntries = true),
        CacheEvict(value = ["cursos:profesor"], allEntries = true),
    ])
    override fun delete(id: Long) {
        val updated = cursoRepository.darDeBajaDirecto(id)
        if (updated == 0) throw NotFoundException("No se encontró el curso con ID: $id o ya está dado de baja")
    }

    @Transactional
    @Caching(evict = [
        CacheEvict(value = ["cursos:lista"],    key = "'todos'"),
        CacheEvict(value = ["cursos:detalle"],  key = "#cursoId"),
        CacheEvict(value = ["cursos:profesor"], allEntries = true),
    ])
    fun reemplazarProfesoresPorId(cursoId: Long, profesoresId: List<Long>): Curso {
        val curso = getById(cursoId)
        val nuevosProfesores = profesoresId
            .map { usuarioService.getById(it).getRolProfesor() }
            .toMutableSet()
        // Sincronizar bidireccional
        curso.profesores.toList().forEach { if (!nuevosProfesores.contains(it)) curso.removerProfesor(it) }
        nuevosProfesores.forEach { if (!curso.esProfesor(it)) curso.agregarProfesor(it) }
        usuarioService.actualizarEstadoProfesor(nuevosProfesores)
        return save(curso)
    }

    @Transactional
    @Caching(evict = [
        CacheEvict(value = ["cursos:lista"],   key = "'todos'"),
        CacheEvict(value = ["cursos:detalle"], key = "#cursoId"),
    ])
    fun actualizarHorariosCurso(cursoId: Long, nuevosHorarios: Set<Horario>): Curso {
        val curso = getById(cursoId)
        curso.horarios.clear()
        curso.horarios.addAll(nuevosHorarios)
        return save(curso)
    }

    @Transactional
    @Caching(evict = [
        CacheEvict(value = ["cursos:lista"],   key = "'todos'"),
        CacheEvict(value = ["cursos:detalle"], key = "#cursoId"),
    ])
    fun actualizarMontosTiposPagoCurso(cursoId: Long, nuevosTiposPago: Set<TipoPago>): Curso {
        val curso = getById(cursoId)
        curso.tiposPago.clear()
        curso.tiposPago.addAll(nuevosTiposPago)
        return save(curso)
    }

    @Transactional
    @Caching(evict = [
        CacheEvict(value = ["cursos:lista"],   key = "'todos'"),
        CacheEvict(value = ["cursos:detalle"], key = "#cursoId"),
    ])
    fun finalizarAltaCursoAlquiler(cursoId: Long, tiposPago: Set<TipoPago>, recargo: Double?): Curso {
        val curso = getById(cursoId)
        curso.tiposPago = tiposPago.toMutableSet()
        curso.recargoAtraso =
            recargo?.let { BigDecimal.valueOf(it).divide(BigDecimal.valueOf(100)).add(BigDecimal.ONE) }
                ?: BigDecimal.ONE
        curso.estadoAlta = EstadoType.ACTIVO
        return save(curso)
    }

    @Transactional
    @Caching(evict = [
        CacheEvict(value = ["cursos:detalle"],    key = "#cursoId"),
        CacheEvict(value = ["cursos:asistencia"], key = "#cursoId"),
    ])
    fun tomarAsistenciaAutomatica(cursoId: Long, usuarioId: Long, fecha: LocalDate?): Curso {
        val curso = getById(cursoId)
        val usuario = usuarioService.getById(usuarioId)
        val fechaAsistencia = fecha ?: AppTime.hoy()

        val alumnosPresentesIds = curso.getInscripcionesActivas()
            .map { it.alumno }
            .filter { it.usuario.tuvoAccesoEnFecha(fechaAsistencia) }
            .map { it.id }
            .toSet()

        curso.tomarAsistencia(usuario, alumnosPresentesIds, fechaAsistencia)
        return save(curso)
    }
}