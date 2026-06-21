package com.estonianport.centro_sis.mapper

import com.estonianport.centro_sis.dto.request.CursoAlquilerAdminRequestDto
import com.estonianport.centro_sis.dto.request.CursoComisionRequestDto
import com.estonianport.centro_sis.dto.response.CursoAlumnoInscriptoDto
import com.estonianport.centro_sis.dto.response.CursoAlumnoResponseDto
import com.estonianport.centro_sis.dto.response.CursoDetalleDto
import com.estonianport.centro_sis.dto.response.CursoResponseDto
import com.estonianport.centro_sis.dto.response.CursoResumenDto
import com.estonianport.centro_sis.dto.response.MiInscripcionCursoDto
import com.estonianport.centro_sis.dto.response.ProfesorResumenDto
import com.estonianport.centro_sis.mapper.HorarioMapper.ordenarPorDia
import com.estonianport.centro_sis.mapper.UsuarioMapper.buildAdultoResponsable
import com.estonianport.centro_sis.model.Curso
import com.estonianport.centro_sis.model.CursoAlquiler
import com.estonianport.centro_sis.model.CursoComision
import com.estonianport.centro_sis.model.Inscripcion
import com.estonianport.centro_sis.model.RolProfesor
import com.estonianport.centro_sis.model.enums.EstadoType
import java.math.BigDecimal
import java.time.LocalDate

object CursoMapper {

    @Deprecated(
        message = "No utilizar más este mapeo estructurado. Migrar al flujo de CursoResumenDto para evitar sobrecarga en memoria.",
        level = DeprecationLevel.WARNING
    )
    fun buildCursoResponseDto(curso: Curso): CursoResponseDto {

        val inscripcionesActivas = curso.inscripciones
            .filter { it.estado == EstadoType.ACTIVO }
            .sortedWith(compareBy({ it.alumno.usuario.nombre }, { it.alumno.usuario.apellido }))

        return CursoResponseDto(
            id = curso.id,
            nombre = curso.nombre,
            horarios = curso.horarios.map { HorarioMapper.buildHorarioResponseDto(it) }.toSet(),
            alumnosInscriptos = inscripcionesActivas.map { UsuarioMapper.buildAlumno(it.alumno.usuario) },
            fechaInicio = curso.fechaInicio.toString(),
            fechaFin = curso.fechaFin.toString(),
            estado = curso.estado.name,
            estadoAlta = curso.estadoAlta.name,
            profesores = curso.profesores.map { UsuarioMapper.buildUsuarioResponseDto(it.usuario) }.toSet(),
            tiposPago = curso.tiposPago.map { TipoPagoMapper.buildTipoPagoResponseDto(it) },
            inscripciones = inscripcionesActivas.map { InscripcionMapper.buildInscripcionResponseDto(it) },
            recargoPorAtraso = curso.recargoAtraso.minus(BigDecimal.ONE).multiply(BigDecimal(100)).toDouble(),
            tipoCurso = curso.tipoCurso.name,
            montoAlquiler = if (curso is CursoAlquiler) curso.precioAlquiler.toDouble() else null,
            cuotasAlquiler = if (curso is CursoAlquiler) curso.cuotasAlquiler else null
        )
    }

    fun buildCursoAlumnoResponseDto(inscripcion: Inscripcion): CursoAlumnoResponseDto {
        return CursoAlumnoResponseDto(
            id = inscripcion.curso.id,
            nombre = inscripcion.curso.nombre,
            horarios = inscripcion.curso.horarios.map { HorarioMapper.buildHorarioResponseDto(it) }.toSet(),
            tipoPagoElegido = TipoPagoMapper.buildTipoPagoResponseDto(inscripcion.tipoPagoSeleccionado),
            profesores = inscripcion.curso.profesores.map { UsuarioMapper.buildUsuarioResponseDto(it.usuario) }.toSet(),
            beneficio = inscripcion.beneficio,
            estadoPago = inscripcion.estadoPago.name,
            alumnosInscriptos = listOf(UsuarioMapper.buildAlumno(inscripcion.alumno.usuario)),
            fechaInicio = inscripcion.curso.fechaInicio.toString(),
            fechaFin = inscripcion.curso.fechaFin.toString(),
            estado = inscripcion.curso.estado.name,
            estadoAlta = inscripcion.curso.estadoAlta.name,
            tiposPago = inscripcion.curso.tiposPago.map { TipoPagoMapper.buildTipoPagoResponseDto(it) },
            inscripciones = inscripcion.curso.inscripciones.map { InscripcionMapper.buildInscripcionResponseDto(it) },
            recargoPorAtraso = inscripcion.curso.recargoAtraso
                .minus(BigDecimal.ONE)
                .multiply(BigDecimal(100))
                .toDouble(),
            tipoCurso = inscripcion.curso.tipoCurso.name,
            fechaInscripcion = inscripcion.fechaInscripcion.toString(),
            porcentajeAsistencia = inscripcion.curso.getPorcentajeAsistenciaAlumno(inscripcion.alumno.id),
            pagosRealizados = inscripcion.pagos.map { PagoMapper.buildPagoResponseDto(it) },
            puntos = inscripcion.puntos,
        )
    }

    fun buildCursoAlquiler(
        cursoDto: CursoAlquilerAdminRequestDto,
        profesores: MutableSet<RolProfesor> = mutableSetOf()
    ): CursoAlquiler {
        return CursoAlquiler(
            id = cursoDto.id,
            nombre = cursoDto.nombre,
            profesores = profesores,
            precioAlquiler = cursoDto.montoAlquiler.toBigDecimal(),
            cuotasAlquiler = cursoDto.cuotasAlquiler,
            fechaInicio = LocalDate.parse(cursoDto.fechaInicio),
            fechaFin = LocalDate.parse(cursoDto.fechaFin),
            horarios = cursoDto.horarios.map { HorarioMapper.buildHorario(it) }.toMutableSet(),
        )
    }

    fun buildCursoComision(
        cursoDto: CursoComisionRequestDto,
        profesores: MutableSet<RolProfesor> = mutableSetOf()
    ): CursoComision {
        return CursoComision(
            id = cursoDto.id,
            nombre = cursoDto.nombre,
            profesores = profesores,
            horarios = cursoDto.horarios.map { HorarioMapper.buildHorario(it) }.toMutableSet(),
            tiposPago = cursoDto.tipoPago.map { TipoPagoMapper.buildTipoPago(it) }.toMutableSet(),
            recargoAtraso = cursoDto.recargo
                ?.toBigDecimal()
                ?.divide(BigDecimal(100))
                ?.add(BigDecimal.ONE)
                ?: BigDecimal.ONE,
            porcentajeComision = cursoDto.comisionProfesor
                ?.toBigDecimal()
                ?.divide(BigDecimal(100))
                ?: BigDecimal("0.50"),
            fechaInicio = LocalDate.parse(cursoDto.fechaInicio),
            fechaFin = LocalDate.parse(cursoDto.fechaFin),
        )
    }

    /**
     * Mappers nuevos de la arquitectura UI-First, como extension functions.
     *
     * Reemplazan a CursoMapper.buildCursoResponseDto / buildCursoAlumnoResponseDto
     * (deprecados). Se mantienen como funciones de extensión —no como objeto
     * estático— a propósito: cada función mapea UN caso de uso concreto, recibe
     * solo los datos extra que necesita (ej. el conteo de alumnos ya resuelto
     * por separado) y no intenta ser "la" función universal de mapeo de Curso.
     *
     * Reutilizan HorarioMapper / TipoPagoMapper porque esos sí son mappers de
     * value objects acotados (no "God DTOs"): no hace falta reinventarlos.
     */

    private fun Curso.profesoresResumen(): List<ProfesorResumenDto> =
        profesores.map {
            val usuario = it.usuario
            ProfesorResumenDto(id = usuario.id, nombreCompleto = "${usuario.nombre} ${usuario.apellido}")
        }

    private fun BigDecimal.aPorcentajeRecargo(): Double =
        minus(BigDecimal.ONE).multiply(BigDecimal(100)).toDouble()

    /** Mapea un Curso a su DTO de listado. [cantidadAlumnos] se resuelve aparte (conteo, no fetch). */
    fun Curso.toResumenDto(cantidadAlumnos: Int): CursoResumenDto = CursoResumenDto(
        id = id,
        nombre = nombre,
        tipoCurso = tipoCurso.name,
        estado = estado.name,
        estadoAlta = estadoAlta.name,
        fechaInicio = fechaInicio.toString(),
        fechaFin = fechaFin.toString(),
        horarios = horarios.ordenarPorDia().map { HorarioMapper.buildHorarioResponseDto(it) },
        profesores = profesoresResumen(),
        cantidadAlumnosInscriptos = cantidadAlumnos
    )

    /** Mapea un Curso a su DTO de detalle. [cantidadAlumnos] se resuelve aparte (conteo, no fetch). */
    fun Curso.toDetalleDto(cantidadAlumnos: Int): CursoDetalleDto = CursoDetalleDto(
        id = id,
        nombre = nombre,
        tipoCurso = tipoCurso.name,
        estado = estado.name,
        estadoAlta = estadoAlta.name,
        fechaInicio = fechaInicio.toString(),
        fechaFin = fechaFin.toString(),
        recargoPorAtrasoPorcentaje = recargoAtraso.aPorcentajeRecargo(),
        horarios = horarios.map { HorarioMapper.buildHorarioResponseDto(it) }.toSet(),
        tiposPago = tiposPago.map { TipoPagoMapper.buildTipoPagoResponseDto(it) },
        profesores = profesoresResumen(),
        montoAlquiler = (this as? CursoAlquiler)?.precioAlquiler?.toDouble(),
        cuotasAlquiler = (this as? CursoAlquiler)?.cuotasAlquiler,
        totalAlumnosInscriptos = cantidadAlumnos
    )

    /** Fila del sub-recurso paginado GET /curso/{id}/alumnos. */
    fun Inscripcion.toAlumnoInscriptoDto(): CursoAlumnoInscriptoDto = CursoAlumnoInscriptoDto(
        inscripcionId = id,
        id = alumno.id,
        nombreCompleto = "${alumno.usuario.nombre} ${alumno.usuario.apellido}",
        email = alumno.usuario.email,
        dni = alumno.usuario.dni,
        celular = alumno.usuario.celular.toString(),
        fechaNacimiento = alumno.usuario.fechaNacimiento.toString(),
        estadoPago = estadoPago.name,
        fechaInscripcion = fechaInscripcion.toString(),
        porcentajeAsistencia = curso.getPorcentajeAsistenciaAlumno(alumno.id),
        puntos = puntos,
        adultoResponsable = alumno.usuario.adultoResponsable?.let { buildAdultoResponsable(it) })

    /** Vista "mi curso" de un alumno puntual. Reemplaza a CursoMapper.buildCursoAlumnoResponseDto. */
    fun Inscripcion.toMiInscripcionDto(): MiInscripcionCursoDto = MiInscripcionCursoDto(
        id = curso.id,
        nombreCurso = curso.nombre,
        estado = curso.estado.name,
        estadoPago = estadoPago.name,
        tipoPagoElegido = TipoPagoMapper.buildTipoPagoResponseDto(tipoPagoSeleccionado),
        fechaInscripcion = fechaInscripcion.toString(),
        porcentajeAsistencia = curso.getPorcentajeAsistenciaAlumno(alumno.id),
        puntos = puntos,
        beneficio = beneficio
    )

}

