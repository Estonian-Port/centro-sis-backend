package com.estonianport.centro_sis.service

import com.estonianport.centro_sis.common.AppTime
import com.estonianport.centro_sis.common.GenericServiceImpl
import com.estonianport.centro_sis.common.errors.NotFoundException
import com.estonianport.centro_sis.dto.request.CursoAlquilerAdminRequestDto
import com.estonianport.centro_sis.dto.request.CursoComisionRequestDto
import com.estonianport.centro_sis.dto.response.CursoResponseDto
import com.estonianport.centro_sis.mapper.CursoMapper
import com.estonianport.centro_sis.model.Curso
import com.estonianport.centro_sis.model.Horario
import com.estonianport.centro_sis.model.ParteAsistencia
import com.estonianport.centro_sis.model.RolProfesor
import com.estonianport.centro_sis.model.enums.TipoPago
import com.estonianport.centro_sis.model.enums.EstadoType
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

    @Autowired
    private lateinit var usuarioService: UsuarioService

    @Autowired
    lateinit var cursoRepository: CursoRepository

    override val dao: CursoRepository
        get() = cursoRepository

    fun countCursos(): Long = cursoRepository.countByFechaBajaIsNull()

    @Cacheable(value = ["cursos:lista"], key = "'todos'")
    fun getAllCursosResponse(): List<CursoResponseDto> {
        // 1. Buscas las entidades
        val cursos = cursoRepository.findAllOrdenadosConDetalles().distinct()
        if (cursos.isEmpty()) return emptyList()

        // 2. Fetch de relaciones
        cursoRepository.findCursosConInscripcionesByIdsIn(cursos.map { it.id })

        // 3. Mapeas a DTO ANTES de retornar y guardar en caché
        return cursos.map { curso ->
            CursoMapper.buildCursoResponseDto(curso)
        }
    }

    @Cacheable(value = ["cursos:detalle"], key = "#id")
    fun getById(id: Long): Curso {
        cursoRepository.findByIdConDetalles(id)
            .orElseThrow { NotFoundException("No se encontró el curso con ID: $id") }
        return cursoRepository.findInscripcionesByCursoId(id)
            .orElseThrow { NotFoundException("No se encontró el curso con ID: $id") }
    }

    @Cacheable(value = ["cursos:profesor"], key = "#idProfe")
    fun obtenerCursosProfesorId(idProfe: Long): List<Curso> =
        cursoRepository.findCursosActivosPorProfesorId(idProfe)

    @Cacheable(value = ["cursos:asistencia"], key = "#cursoId")
    fun getPartesAsistencia(cursoId: Long): List<ParteAsistencia> =
        cursoRepository.findPartesAsistenciaByCursoId(cursoId)

    // ─── ESCRITURAS ───────────────────────────────────────────────────────────

    @Transactional
    @Caching(evict = [
        CacheEvict(value = ["cursos:lista"], key = "'todos'"),
        CacheEvict(value = ["cursos:detalle"], key = "#nuevoCurso.id", condition = "#nuevoCurso.id != 0")
    ])
    fun alta(nuevoCurso: Curso): Curso = cursoRepository.save(nuevoCurso)

    @Transactional
    @Caching(evict = [
        CacheEvict(value = ["cursos:lista"], key = "'todos'")
    ])
    fun crearCursoAlquiler(cursoRequestDto: CursoAlquilerAdminRequestDto): Curso {
        val profesores = cursoRequestDto.profesoresId
            .map { usuarioService.getById(it).getRolProfesor() }
            .toMutableSet()

        val nuevoCursoAlquiler = CursoMapper.buildCursoAlquiler(cursoRequestDto, profesores)
        val cursoAgregado = alta(nuevoCursoAlquiler)
        usuarioService.actualizarEstadoProfesor(profesores)
        return cursoAgregado
    }

    @Transactional
    @Caching(evict = [
        CacheEvict(value = ["cursos:lista"], key = "'todos'")
    ])
    fun crearCursoComision(cursoRequestDto: CursoComisionRequestDto): Curso {
        val profesores = cursoRequestDto.profesoresId
            .map { usuarioService.getById(it).getRolProfesor() }
            .toMutableSet()

        val nuevoCursoComision = CursoMapper.buildCursoComision(cursoRequestDto, profesores)
        nuevoCursoComision.estadoAlta = EstadoType.ACTIVO
        val cursoAgregado = alta(nuevoCursoComision)
        usuarioService.actualizarEstadoProfesor(profesores)
        return cursoAgregado
    }

    @Transactional
    @Caching(evict = [
        CacheEvict(value = ["cursos:lista"], key = "'todos'"),
        CacheEvict(value = ["cursos:detalle"], key = "#cursoId")
    ])
    fun actualizarNombreCurso(cursoId: Long, nuevoNombre: String): Curso {
        val updated = cursoRepository.updateNombre(cursoId, nuevoNombre)
        if (updated == 0) throw NotFoundException("No se encontró el curso con ID: $cursoId")
        return getById(cursoId)
    }

    @Transactional
    @Caching(evict = [
        CacheEvict(value = ["cursos:lista"], key = "'todos'"),
        CacheEvict(value = ["cursos:detalle"], key = "#id"),
        CacheEvict(value = ["cursos:profesor"], allEntries = true)
    ])
    override fun delete(id: Long) {
        val updated = cursoRepository.darDeBajaDirecto(id)
        if (updated == 0) throw NotFoundException("No se encontró el curso con ID: $id o ya está dado de baja")
    }

    @Transactional
    @Caching(evict = [
        CacheEvict(value = ["cursos:lista"], key = "'todos'"),
        CacheEvict(value = ["cursos:detalle"], key = "#curso.id"),
        CacheEvict(value = ["cursos:profesor"], allEntries = true)
    ])
    fun actualizarProfesoresDelCurso(curso: Curso, nuevosProfesores: Set<RolProfesor>): Curso {
        curso.profesores.clear()
        curso.profesores.addAll(nuevosProfesores)
        return save(curso)
    }

    @Transactional
    @Caching(evict = [
        CacheEvict(value = ["cursos:lista"], key = "'todos'"),
        CacheEvict(value = ["cursos:detalle"], key = "#cursoId"),
        CacheEvict(value = ["cursos:profesor"], allEntries = true)
    ])
    fun actualizarProfesores(cursoId: Long, nuevosProfesores: Set<RolProfesor>): Curso {
        val curso = getById(cursoId)
        nuevosProfesores.forEach { if (!curso.esProfesor(it)) curso.agregarProfesor(it) }
        curso.profesores.toList().forEach { if (!nuevosProfesores.contains(it)) curso.removerProfesor(it) }
        return save(curso)
    }

    @Transactional
    @Caching(evict = [
        CacheEvict(value = ["cursos:lista"], key = "'todos'"),
        CacheEvict(value = ["cursos:detalle"], key = "#cursoId"),
        CacheEvict(value = ["cursos:profesor"], allEntries = true)
    ])
    fun reemplazarProfesoresPorId(cursoId: Long, profesoresId: List<Long>): Curso {
        val curso = getById(cursoId)
        val nuevosProfesores = profesoresId
            .map { usuarioService.getById(it).getRolProfesor() }
            .toMutableSet()
        usuarioService.actualizarEstadoProfesor(nuevosProfesores)
        return actualizarProfesoresDelCurso(curso, nuevosProfesores)
    }

    @Transactional
    @Caching(evict = [
        CacheEvict(value = ["cursos:lista"], key = "'todos'"),
        CacheEvict(value = ["cursos:detalle"], key = "#cursoId")
    ])
    fun actualizarHorariosCurso(cursoId: Long, nuevosHorarios: Set<Horario>): Curso {
        val curso = getById(cursoId)
        curso.horarios.clear()
        curso.horarios.addAll(nuevosHorarios)
        return save(curso)
    }

    @Transactional
    @Caching(evict = [
        CacheEvict(value = ["cursos:lista"], key = "'todos'"),
        CacheEvict(value = ["cursos:detalle"], key = "#cursoId")
    ])
    fun actualizarMontosTiposPagoCurso(cursoId: Long, nuevosTiposPago: Set<TipoPago>): Curso {
        val curso = getById(cursoId)
        curso.tiposPago.clear()
        curso.tiposPago.addAll(nuevosTiposPago)
        return save(curso)
    }

    @Transactional
    @Caching(evict = [
        CacheEvict(value = ["cursos:lista"], key = "'todos'"),
        CacheEvict(value = ["cursos:detalle"], key = "#cursoId")
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
        CacheEvict(value = ["cursos:detalle"], key = "#cursoId"),
        CacheEvict(value = ["cursos:asistencia"], key = "#cursoId")
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