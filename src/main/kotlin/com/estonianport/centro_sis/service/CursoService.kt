package com.estonianport.centro_sis.service

import com.estonianport.centro_sis.common.AppTime
import com.estonianport.centro_sis.common.GenericServiceImpl
import com.estonianport.centro_sis.common.errors.NotFoundException
import com.estonianport.centro_sis.dto.request.CursoAlquilerAdminRequestDto
import com.estonianport.centro_sis.dto.request.CursoComisionRequestDto
import com.estonianport.centro_sis.dto.response.CursoResponseDto
import com.estonianport.centro_sis.dto.response.PageResponse
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
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDate

@Service
@Transactional(readOnly = true)
class CursoService(
    private val usuarioService: UsuarioService,
    private val cursoRepository: CursoRepository
) {

    fun countCursos(): Long = cursoRepository.countByFechaBajaIsNull()

    // ─── LECTURAS ─────────────────────────────────────────────────────────────

    /**
     * Lista completa (sin paginación) — conservada para endpoints internos y
     * para la vista de calendario que necesita todos los cursos a la vez.
     */
    @Cacheable(value = ["cursos:lista"], key = "'todos'")
    fun getAllCursosResponse(): List<CursoResponseDto> {
        val cursos = cursoRepository.findAllOrdenadosConDetalles().distinct()
        if (cursos.isEmpty()) return emptyList()
        cursoRepository.findCursosConInscripcionesByIdsIn(cursos.map { it.id })
        return cursos.map { CursoMapper.buildCursoResponseDto(it) }
    }

    /**
     * Versión paginada para la pantalla de administración.
     *
     * Usa el patrón "IDs primero, hidratación después" — igual que PagoService —
     * para evitar el producto cartesiano que rompía el conteo de páginas cuando
     * se combinaban JOIN FETCH con Pageable.
     *
     * El filtro de [estadoCurso] (POR_COMENZAR / EN_CURSO / FINALIZADO) se aplica
     * en memoria sobre la página pequeña (≤ 10 registros) porque ese estado es
     * calculado en Kotlin a partir de fechaInicio/fechaFin, no persiste en la DB.
     * El filtro de [estadoAlta] (ACTIVO / PENDIENTE / BAJA) sí va a la DB.
     */
    fun getAllCursosPaginado(
        page: Int,
        size: Int,
        search: String?,
        estadoAlta: EstadoType?,
        estadoCurso: String?          // "POR_COMENZAR" | "EN_CURSO" | "FINALIZADO"
    ): Page<CursoResponseDto> {
        val pageable = PageRequest.of(page, size)

        // 1. IDs paginados con filtros que la DB puede resolver
        val idsPage: Page<Long> = cursoRepository.findIdsPaginados(
            search?.takeIf { it.isNotBlank() },
            estadoAlta,
            pageable
        )

        if (idsPage.isEmpty) return Page.empty(pageable)

        // 2. Hidratar con detalles (profesores, horarios, tiposPago)
        val cursosMap = cursoRepository.findConDetallesByIds(idsPage.content)
            .associateBy { it.id }

        // 3. Segunda pasada: inscripciones (evita N+1 y producto cartesiano)
        cursoRepository.findCursosConInscripcionesByIdsIn(idsPage.content)

        // 4. Mapear respetando orden + filtrar por estadoCurso en memoria
        var cursos = idsPage.content.mapNotNull { cursosMap[it] }

        if (!estadoCurso.isNullOrBlank()) {
            cursos = cursos.filter { it.estado.name == estadoCurso }
        }

        val dtos = cursos.map { CursoMapper.buildCursoResponseDto(it) }
        return PageImpl(dtos, pageable, idsPage.totalElements)
    }

    fun getById(id: Long): Curso {
        cursoRepository.findByIdConDetalles(id)
            .orElseThrow { NotFoundException("No se encontró el curso con ID: $id") }
        return cursoRepository.findInscripcionesByCursoId(id)
            .orElseThrow { NotFoundException("No se encontró el curso con ID: $id") }
    }

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
    fun delete(id: Long) {
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
        curso.profesores.toList().forEach { if (!nuevosProfesores.contains(it)) curso.removerProfesor(it) }
        nuevosProfesores.forEach { if (!curso.esProfesor(it)) curso.agregarProfesor(it) }
        usuarioService.actualizarEstadoProfesor(nuevosProfesores)
        return cursoRepository.save(curso)
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
        return cursoRepository.save(curso)
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
        return cursoRepository.save(curso)
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
        return cursoRepository.save(curso)
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
        return cursoRepository.save(curso)
    }
}
