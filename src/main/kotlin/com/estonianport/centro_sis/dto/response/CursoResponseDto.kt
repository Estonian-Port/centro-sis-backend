package com.estonianport.centro_sis.dto.response

import com.estonianport.centro_sis.dto.HorarioDto
import com.estonianport.centro_sis.dto.request.AdultoResponsableDto
import com.estonianport.centro_sis.model.AdultoResponsable
import java.io.Serializable

data class CursoResponseDto(
    val id: Long,
    val nombre: String,
    val horarios: Set<HorarioDto>,
    val alumnosInscriptos: List<AlumnoResponseDto>,
    val fechaInicio: String,
    val fechaFin: String,
    val estado: String,
    val estadoAlta: String,
    val profesores: Set<UsuarioResponseDto>,
    val tiposPago: List<TipoPagoDto>,
    val inscripciones: List<InscripcionResponseDto>,
    val recargoPorAtraso: Double,
    val tipoCurso: String,
    val montoAlquiler: Double?,
    val cuotasAlquiler: Int?,
): Serializable

data class CursoAlumnoResponseDto(
    val id: Long,
    val nombre: String,
    val horarios: Set<HorarioDto>,
    val alumnosInscriptos: List<AlumnoResponseDto>,
    val fechaInicio: String,
    val fechaFin: String,
    val estado: String,
    val estadoAlta: String,
    val profesores: Set<UsuarioResponseDto>,
    val tiposPago: List<TipoPagoDto>,
    val inscripciones: List<InscripcionResponseDto>,
    val recargoPorAtraso: Double,
    val tipoCurso: String,
    val estadoPago: String,
    val tipoPagoElegido: TipoPagoDto,
    val fechaInscripcion: String,
    val porcentajeAsistencia: Double,
    val beneficio: Int,
    val pagosRealizados: List<PagoResponseDto>,
    val puntos: Int,
): Serializable


/**
 * DTO "UI-First" para pantallas de LISTADO (admin paginado, calendario).
 *
 * A diferencia de CursoResponseDto (deprecado), NO trae:
 *  - alumnosInscriptos completos        -> solo cantidadAlumnosInscriptos
 *  - inscripciones completas            -> eliminado por completo
 *  - tiposPago / recargo                -> no se muestran en una grilla/calendario
 *
 * Si una pantalla concreta necesita 1-2 campos más, agregalos acá; lo que
 * NO debe pasar es que vuelva a cargar colecciones de entidades relacionadas.
 */
data class CursoResumenDto(
    val id: Long,
    val nombre: String,
    val tipoCurso: String,
    val estado: String,
    val estadoAlta: String,
    val fechaInicio: String,
    val fechaFin: String,
    val horarios: List<HorarioDto>,
    val profesores: List<ProfesorResumenDto>,
    val cantidadAlumnosInscriptos: Int
) : Serializable

/**
 * Versión "shallow" de un profesor: solo lo que una lista necesita mostrar.
 * Evita devolver UsuarioResponseDto completo (con todos sus campos/relaciones)
 * dentro de una colección.
 */
data class ProfesorResumenDto(
    val id: Long,
    val nombreCompleto: String
) : Serializable

data class CursoDetalleDto(
    val id: Long,
    val nombre: String,
    val tipoCurso: String,
    val estado: String,
    val estadoAlta: String,
    val fechaInicio: String,
    val fechaFin: String,
    val recargoPorAtrasoPorcentaje: Double,
    val horarios: Set<HorarioDto>,
    val tiposPago: List<TipoPagoDto>,
    val profesores: List<ProfesorResumenDto>,
    val montoAlquiler: Double?,
    val cuotasAlquiler: Int?,
    val totalAlumnosInscriptos: Int
) : Serializable

/**
 * Fila de la tabla paginada de alumnos inscriptos en un curso.
 *
 * Sub-recurso de CursoDetalleDto: GET /curso/{cursoId}/alumnos?page=&size=
 *
 * Reemplaza al campo `alumnosInscriptos` / `inscripciones` que antes venía
 * embebido en CursoResponseDto. Al paginarse de forma independiente, un
 * curso con 500 alumnos ya no rompe ni el listado ni el detalle del curso.
 */
data class CursoAlumnoInscriptoDto(
    val id: Long,
    val inscripcionId: Long,
    val nombreCompleto: String,
    val email : String,
    val dni : String,
    val celular : String,
    val fechaNacimiento : String,
    val estadoPago: String,
    val fechaInscripcion: String,
    val porcentajeAsistencia: Double,
    val puntos: Int,
    val adultoResponsable: AdultoResponsableDto?
) : Serializable

/**
 * Reemplazo granular de CursoAlumnoResponseDto (deprecado).
 *
 * Es la vista de "mi curso" desde la perspectiva de UN alumno puntual.
 * GET /curso/{cursoId}/mi-inscripcion?alumnoId=...
 *
 * A diferencia del DTO viejo, NO incluye:
 *  - inscripciones: List<InscripcionResponseDto>  -> no tiene sentido acá,
 *      el alumno no necesita ver la lista de inscripciones de SUS compañeros
 *  - alumnosInscriptos: List<AlumnoResponseDto>    -> idem
 *  - pagosRealizados: List<PagoResponseDto>        -> si la pantalla de pagos
 *      lo necesita, debería ser su propio endpoint paginado
 *      (ej. GET /curso/{cursoId}/mi-inscripcion/pagos), ya que un historial
 *      de pagos también puede crecer indefinidamente.
 */
data class MiInscripcionCursoDto(
    val id: Long,
    val nombreCurso: String,
    val estado: String,
    val estadoPago: String,
    val tipoPagoElegido: TipoPagoDto,
    val fechaInscripcion: String,
    val porcentajeAsistencia: Double,
    val puntos: Int,
    val beneficio: Int,
) : Serializable

/**
 * Proyección liviana usada por CursoRepository.countAlumnosActivosPorCursoIds.
 *
 * Permite saber "cuántos alumnos tiene cada curso" sin traer las entidades
 * Inscripcion/Alumno/Usuario completas — esa era la causa principal del
 * over-fetching en los listados (findCursosConInscripcionesByIdsIn se
 * llamaba aunque la pantalla solo necesitara un número).
 */
data class CursoConteoAlumnosDto(
    val id: Long,
    val cantidad: Long
)

