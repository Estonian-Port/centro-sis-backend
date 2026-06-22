package com.estonianport.centro_sis.service

import com.estonianport.centro_sis.common.AppTime
import com.estonianport.centro_sis.common.errors.NotFoundException
import com.estonianport.centro_sis.dto.request.CursoAlquilerAdminRequestDto
import com.estonianport.centro_sis.dto.request.CursoComisionRequestDto
import com.estonianport.centro_sis.dto.response.CursoAlumnoInscriptoDto
import com.estonianport.centro_sis.dto.response.CursoDetalleDto
import com.estonianport.centro_sis.dto.response.CursoProfesorSummaryDto
import com.estonianport.centro_sis.dto.response.CursoResponseDto
import com.estonianport.centro_sis.dto.response.CursoResumenDto
import com.estonianport.centro_sis.dto.response.MiInscripcionCursoDto
import com.estonianport.centro_sis.dto.response.PageResponse
import com.estonianport.centro_sis.dto.response.PagoRealizadoDto
import com.estonianport.centro_sis.dto.response.ParteAsistenciaResponseDto
import com.estonianport.centro_sis.mapper.CursoMapper
import com.estonianport.centro_sis.mapper.CursoMapper.toAlumnoInscriptoDto
import com.estonianport.centro_sis.mapper.CursoMapper.toDetalleDto
import com.estonianport.centro_sis.mapper.CursoMapper.toMiInscripcionDto
import com.estonianport.centro_sis.mapper.CursoMapper.toResumenDto
import com.estonianport.centro_sis.mapper.PagoMapper.toPagoRealizadoDto
import com.estonianport.centro_sis.mapper.ParteAsistenciaMapper
import com.estonianport.centro_sis.model.Curso
import com.estonianport.centro_sis.model.Horario
import com.estonianport.centro_sis.model.enums.EstadoType
import com.estonianport.centro_sis.model.enums.TipoPago
import com.estonianport.centro_sis.repository.CursoRepository
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

    @Deprecated(message = "Usar getActivosResumen en su lugar. Evita el fetch de colecciones pesadas utilizando agregaciones.",)
    @Cacheable(value = ["cursos:lista"], key = "'todos'")
    fun getAllCursosResponse(): List<CursoResponseDto> {
        val cursos = cursoRepository.findAllOrdenadosConDetalles().distinct()
        if (cursos.isEmpty()) return emptyList()
        cursoRepository.findCursosConInscripcionesByIdsIn(cursos.map { it.id })
        return cursos.map { CursoMapper.buildCursoResponseDto(it) }
    }

    @Deprecated(message = "Usar getResumenPaginado en su lugar. Trae DTO resumido y cuenta alumnos de forma eficiente sin mutar colecciones.",)
    fun getAllCursosPaginado(
        page: Int,
        size: Int,
        search: String?,
        estadoAlta: EstadoType?,
        estadoCurso: String?
    ): PageResponse<CursoResponseDto> {
        val pageable = PageRequest.of(page, size)

        val idsPage: Page<Long> = cursoRepository.findIdsPaginados(
            search?.takeIf { it.isNotBlank() },
            estadoAlta,
            pageable
        )
        if (idsPage.isEmpty) return PageResponse.from(Page.empty(pageable))
        val cursosMap = cursoRepository.findConDetallesByIds(idsPage.content)
            .associateBy { it.id }
        cursoRepository.findCursosConInscripcionesByIdsIn(idsPage.content)
        var cursos = idsPage.content.mapNotNull { cursosMap[it] }

        if (!estadoCurso.isNullOrBlank()) {
            cursos = cursos.filter { it.estado.name == estadoCurso }
        }
        val dtos = cursos.map { CursoMapper.buildCursoResponseDto(it) }
        val pageImpl = PageImpl(dtos, pageable, idsPage.totalElements)

        return PageResponse.from(pageImpl)
    }

    // ════════════════════════════════════════════════════════════════════════
    // ─── LECTURAS GRANULARES (UI-First) ──────────────────────────────────────
    // ════════════════════════════════════════════════════════════════════════

    @Cacheable(value = ["cursos:resumen:pagina"], key = "{#page, #size, #search, #estadoAlta, #estadoCurso}")
    fun getResumenPaginado(
        page: Int,
        size: Int,
        search: String?,
        estadoAlta: EstadoType?,
        estadoCurso: String?
    ): PageResponse<CursoResumenDto> {
        val pageable = PageRequest.of(page, size)

        val idsPage: Page<Long> = cursoRepository.findIdsPaginados(
            search?.takeIf { it.isNotBlank() },
            estadoAlta,
            pageable
        )
        if (idsPage.isEmpty) return PageResponse.from(Page.empty(pageable))

        val cursosMap = cursoRepository.findConDetallesByIds(idsPage.content).associateBy { it.id }
        val conteos = cursoRepository.countAlumnosActivosPorCursoIds(idsPage.content)
            .associate { it.id to it.cantidad.toInt() }

        var cursos = idsPage.content.mapNotNull { cursosMap[it] }
        if (!estadoCurso.isNullOrBlank()) {
            cursos = cursos.filter { it.estado.name == estadoCurso }
        }

        val dtos = cursos.map { it.toResumenDto(conteos[it.id] ?: 0) }
        val pageImpl = PageImpl(dtos, pageable, idsPage.totalElements)

        return PageResponse.from(pageImpl)
    }

    @Cacheable(value = ["cursos:resumen:activos"], key = "'todos'")
    fun getActivosResumen(): List<CursoResumenDto> {
        val cursos = cursoRepository.findAllOrdenadosConDetalles().distinct()
        if (cursos.isEmpty()) return emptyList()
        val conteos = cursoRepository.countAlumnosActivosPorCursoIds(cursos.map { it.id })
            .associate { it.id to it.cantidad.toInt() }
        return cursos.map { it.toResumenDto(conteos[it.id] ?: 0) }
    }

    @Cacheable(value = ["cursos:detalle"], key = "#id")
    fun getDetalleDto(id: Long): CursoDetalleDto {
        val curso = cursoRepository.findByIdConDetalles(id)
            .orElseThrow { NotFoundException("No se encontró el curso con ID: $id") }
        val cantidad = cursoRepository.countAlumnosActivosPorCursoIds(listOf(id))
            .firstOrNull()?.cantidad?.toInt() ?: 0
        return curso.toDetalleDto(cantidad)
    }

    @Cacheable(value = ["cursos:alumnos:pagina"], key = "{#cursoId, #page, #size}")
    fun getAlumnosInscriptosPaginado(cursoId: Long, page: Int, size: Int): PageResponse<CursoAlumnoInscriptoDto> {
        val pageable = PageRequest.of(page, size)
        val pageImpl = cursoRepository.findInscripcionesActivasPaginadoByCursoId(cursoId, pageable)
            .map { it.toAlumnoInscriptoDto() }

        return PageResponse.from(pageImpl)
    }

    @Cacheable(value = ["cursos:mi-inscripcion"], key = "{#cursoId, #alumnoId}")
    fun getMiInscripcion(cursoId: Long, alumnoId: Long): MiInscripcionCursoDto {
        val inscripcion = cursoRepository.findInscripcionActivaByCursoIdYAlumnoId(cursoId, alumnoId)
            .orElseThrow {
                NotFoundException("No se encontró inscripción activa del alumno $alumnoId en el curso $cursoId")
            }
        return inscripcion.toMiInscripcionDto()
    }

    @Cacheable(value = ["cursos:mis-pagos"], key = "{#cursoId, #alumnoId}")
    fun getMisPagos(cursoId: Long, alumnoId: Long): List<PagoRealizadoDto> {
        val pagos = cursoRepository.findPagosByCursoIdYAlumnoId(cursoId, alumnoId)
        return pagos.map { it.toPagoRealizadoDto() }
    }

    @Cacheable(value = ["cursos:resumen:profesor"], key = "#idProfe")
    fun getCursosResumenPorProfesorId(idProfe: Long): List<CursoResumenDto> {
        val cursos = cursoRepository.findCursosActivosPorProfesorId(idProfe)
        if (cursos.isEmpty()) return emptyList()
        val conteos = cursoRepository.countAlumnosActivosPorCursoIds(cursos.map { it.id })
            .associate { it.id to it.cantidad.toInt() }
        return cursos.map { it.toResumenDto(conteos[it.id] ?: 0) }
    }

    fun getById(id: Long): Curso {
        cursoRepository.findByIdConDetalles(id)
            .orElseThrow { NotFoundException("No se encontró el curso con ID: $id") }
        return cursoRepository.findInscripcionesByCursoId(id)
            .orElseThrow { NotFoundException("No se encontró el curso con ID: $id") }
    }

    @Deprecated("Utilizar getDetalleDto(id) en su lugar para mejorar el rendimiento.")
    @Cacheable(value = ["cursos:detalle"], key = "#id")
    fun getByIdDto(id: Long): CursoResponseDto =
        CursoMapper.buildCursoResponseDto(getById(id))

    @Deprecated(message = "Usar obtenerCursosProfesorSummary en su lugar. Devuelve DTO optimizado y procesa conteos " +
            "eficientemente.",)
    @Cacheable(value = ["cursos:profesor"], key = "#idProfe")
    fun obtenerCursosProfesorId(idProfe: Long): List<CursoResponseDto> {
        val cursosEntidad = cursoRepository.findCursosActivosPorProfesorId(idProfe)
        return cursosEntidad.map { CursoMapper.buildCursoResponseDto(it) }
    }

    @Transactional(readOnly = true)
    @Cacheable(value = ["cursos:profesor:summary"], key = "#profesorId")
    fun obtenerCursosProfesorSummary(profesorId: Long): List<CursoProfesorSummaryDto> {
        val cursos = cursoRepository.findCursosSummaryPorProfesorId(profesorId)
        if (cursos.isEmpty()) return emptyList()

        val cursosIds = cursos.map { it.id }

        val alumnosPorCursoMap = cursoRepository
            .countAlumnosActivosPorCursoIds(cursosIds)
            .associate { it.id to it.cantidad.toInt() }

        return cursos.map { curso ->
            CursoMapper.buildCursoProfesorSummaryDto(
                curso = curso,
                totalAlumnos = alumnosPorCursoMap[curso.id] ?: 0
            )
        }
    }

    @Cacheable(value = ["cursos:asistencia"], key = "#cursoId")
    fun getPartesAsistencia(cursoId: Long): List<ParteAsistenciaResponseDto> {
        val partes = cursoRepository.findPartesAsistenciaByCursoId(cursoId)
        return partes.map { ParteAsistenciaMapper.buildParteAsistenciaResponseDto(it) }
    }

    // ─── ESCRITURAS ───────────────────────────────────────────────────────────

    @Transactional
    @Caching(evict = [
        CacheEvict(value = ["cursos:resumen:activos"], key = "'todos'"),
        CacheEvict(value = ["cursos:resumen:pagina"], allEntries = true),
    ])
    fun alta(nuevoCurso: Curso): Curso = cursoRepository.save(nuevoCurso)

    @Transactional
    @Caching(evict = [
        CacheEvict(value = ["cursos:resumen:activos"], key = "'todos'"),
        CacheEvict(value = ["cursos:resumen:pagina"], allEntries = true),
        CacheEvict(value = ["cursos:resumen:profesor"], allEntries = true),
        CacheEvict(value = ["cursos:profesor:summary"], allEntries = true)
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
        CacheEvict(value = ["cursos:resumen:activos"], key = "'todos'"),
        CacheEvict(value = ["cursos:resumen:pagina"], allEntries = true),
        CacheEvict(value = ["cursos:resumen:profesor"], allEntries = true),
        CacheEvict(value = ["cursos:profesor:summary"], allEntries = true)
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
        CacheEvict(value = ["cursos:resumen:activos"], key = "'todos'"),
        CacheEvict(value = ["cursos:resumen:pagina"], allEntries = true),
        CacheEvict(value = ["cursos:detalle"], key = "#cursoId"),
        CacheEvict(value = ["cursos:resumen:profesor"], allEntries = true),
        CacheEvict(value = ["cursos:profesor:summary"], allEntries = true)
    ])
    fun actualizarNombreCurso(cursoId: Long, nuevoNombre: String): CursoDetalleDto {
        val updated = cursoRepository.updateNombre(cursoId, nuevoNombre)
        if (updated == 0) throw NotFoundException("No se encontró el curso con ID: $cursoId")
        return getDetalleDto(cursoId)
    }

    @Transactional
    @Caching(evict = [
        CacheEvict(value = ["cursos:resumen:activos"], key = "'todos'"),
        CacheEvict(value = ["cursos:resumen:pagina"], allEntries = true),
        CacheEvict(value = ["cursos:detalle"], allEntries = true),
        CacheEvict(value = ["cursos:resumen:profesor"], allEntries = true),
        CacheEvict(value = ["cursos:profesor:summary"], allEntries = true),
        CacheEvict(value = ["cursos:alumnos:pagina"], allEntries = true),
        CacheEvict(value = ["cursos:mi-inscripcion"], allEntries = true),
    ])
    fun delete(id: Long) {
        val updated = cursoRepository.darDeBajaDirecto(id)
        if (updated == 0) throw NotFoundException("No se encontró el curso con ID: $id o ya está dado de baja")
    }

    @Transactional
    @Caching(evict = [
        CacheEvict(value = ["cursos:resumen:activos"], key = "'todos'"),
        CacheEvict(value = ["cursos:resumen:pagina"], allEntries = true),
        CacheEvict(value = ["cursos:detalle"], key = "#cursoId"),
        CacheEvict(value = ["cursos:resumen:profesor"], allEntries = true),
        CacheEvict(value = ["cursos:profesor:summary"], allEntries = true),
        CacheEvict(value = ["cursos:profesor"], allEntries = true) // Por el endpoint deprecado
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
        CacheEvict(value = ["cursos:resumen:activos"], key = "'todos'"),
        CacheEvict(value = ["cursos:resumen:pagina"], allEntries = true),
        CacheEvict(value = ["cursos:detalle"], key = "#cursoId"),
        CacheEvict(value = ["cursos:resumen:profesor"], allEntries = true),
        CacheEvict(value = ["cursos:profesor:summary"], allEntries = true)
    ])
    fun actualizarHorariosCurso(cursoId: Long, nuevosHorarios: Set<Horario>): Curso {
        val curso = getById(cursoId)
        curso.horarios.removeIf { it !in nuevosHorarios }
        nuevosHorarios.forEach { if (it !in curso.horarios) curso.horarios.add(it) }
        return cursoRepository.save(curso)
    }

    @Transactional
    @Caching(evict = [
        CacheEvict(value = ["cursos:detalle"], key = "#cursoId"),
        CacheEvict(value = ["cursos:resumen:activos"], key = "'todos'"),
        CacheEvict(value = ["cursos:resumen:pagina"], allEntries = true),
        CacheEvict(value = ["cursos:resumen:profesor"], allEntries = true),
        CacheEvict(value = ["cursos:profesor:summary"], allEntries = true)
    ])
    fun actualizarMontosTiposPagoCurso(cursoId: Long, nuevosTiposPago: Set<TipoPago>): Curso {
        val curso = getById(cursoId)
        curso.tiposPago.removeIf { it !in nuevosTiposPago }
        nuevosTiposPago.forEach { if (it !in curso.tiposPago) curso.tiposPago.add(it) }
        return cursoRepository.save(curso)
    }

    @Transactional
    @Caching(evict = [
        CacheEvict(value = ["cursos:resumen:activos"], key = "'todos'"),
        CacheEvict(value = ["cursos:resumen:pagina"], allEntries = true),
        CacheEvict(value = ["cursos:detalle"], key = "#cursoId"),
        CacheEvict(value = ["cursos:resumen:profesor"], allEntries = true),
        CacheEvict(value = ["cursos:profesor:summary"], allEntries = true)
    ])
    fun finalizarAltaCursoAlquiler(cursoId: Long, tiposPago: Set<TipoPago>, recargo: Double?): Curso {
        val curso = getById(cursoId)
        curso.tiposPago.removeIf { it !in tiposPago }
        tiposPago.forEach { if (it !in curso.tiposPago) curso.tiposPago.add(it) }
        curso.recargoAtraso =
            recargo?.let { BigDecimal.valueOf(it).divide(BigDecimal.valueOf(100)).add(BigDecimal.ONE) }
                ?: BigDecimal.ONE
        curso.estadoAlta = EstadoType.ACTIVO
        return cursoRepository.save(curso)
    }

    @Transactional
    @Caching(evict = [
        CacheEvict(value = ["cursos:asistencia"], key = "#cursoId"),
        CacheEvict(value = ["cursos:alumnos:pagina"], allEntries = true),
        CacheEvict(value = ["cursos:mi-inscripcion"], allEntries = true),
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