package com.estonianport.centro_sis.model

import com.estonianport.centro_sis.model.enums.EstadoType
import com.estonianport.centro_sis.model.enums.PagoType
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDate

@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "rol_type", discriminatorType = DiscriminatorType.STRING)
abstract class Rol(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    var usuario: Usuario,

    @Column
    var fechaAlta: LocalDate = LocalDate.now(),

    @Column
    var fechaBaja: LocalDate? = null
) {
    abstract fun puedeGestionarCurso(curso: Curso): Boolean
    abstract fun puedeRegistrarPago(inscripcion: Inscripcion): Boolean
    abstract fun puedeAsignarBeneficio(inscripcion: Inscripcion): Boolean
}

@Entity
@DiscriminatorValue("ADMINISTRADOR")
class RolAdmin(
    usuario: Usuario
) : Rol(usuario = usuario) {

    override fun puedeGestionarCurso(curso: Curso): Boolean = true

    override fun puedeRegistrarPago(inscripcion: Inscripcion): Boolean = true

    override fun puedeAsignarBeneficio(inscripcion: Inscripcion): Boolean = true
}

@Entity
@DiscriminatorValue("OFICINA")
class RolOficina(
    usuario: Usuario
) : Rol(usuario = usuario) {

    override fun puedeGestionarCurso(curso: Curso): Boolean = true

    override fun puedeRegistrarPago(inscripcion: Inscripcion): Boolean = true

    override fun puedeAsignarBeneficio(inscripcion: Inscripcion): Boolean {
        // Solo puede asignar beneficios en cursos por comisión
        return inscripcion.curso is CursoComision
    }
}

@Entity
@DiscriminatorValue("PROFESOR")
class RolProfesor(
    usuario: Usuario,

    @ManyToMany(mappedBy = "profesores", fetch = FetchType.LAZY)
    var cursos: MutableSet<Curso> = mutableSetOf(),

    // Pagos DE alquiler (profesor PAGA al instituto)
    @OneToMany(mappedBy = "profesor", cascade = [CascadeType.ALL])
    val pagosAlquilerRealizados: MutableList<PagoAlquiler> = mutableListOf(),

    // Pagos DE comisión (profesor RECIBE del instituto)
    @OneToMany(mappedBy = "profesor", cascade = [CascadeType.ALL])
    val pagosComisionRecibidos: MutableList<PagoComision> = mutableListOf()
) : Rol(usuario = usuario) {

    override fun puedeGestionarCurso(curso: Curso): Boolean {
        return curso is CursoAlquiler && cursos.contains(curso)
    }

    override fun puedeRegistrarPago(inscripcion: Inscripcion): Boolean {
        return inscripcion.curso is CursoAlquiler &&
                cursos.contains(inscripcion.curso)
    }

    override fun puedeAsignarBeneficio(inscripcion: Inscripcion): Boolean {
        return inscripcion.curso is CursoAlquiler &&
                cursos.contains(inscripcion.curso)
    }

    fun cursosActivos(): List<Curso> {
        return cursos.filter { it.fechaBaja == null }
    }

    // Pagos que el profesor REALIZÓ al instituto (alquileres)
    fun obtenerPagosRealizados(): List<PagoAlquiler> {
        return pagosAlquilerRealizados.filter { it.estaActivo() }
    }

    // Pagos que el profesor RECIBIÓ del instituto (comisiones)
    fun obtenerPagosRecibidos(): List<PagoComision> {
        return pagosComisionRecibidos.filter { it.estaActivo() }
    }

    // Balance del profesor (comisiones recibidas - alquileres pagados)
    fun calcularBalance(): BigDecimal {
        val totalRecibido = obtenerPagosRecibidos().sumOf { it.monto }
        val totalPagado = obtenerPagosRealizados().sumOf { it.monto }
        return totalRecibido - totalPagado
    }
}

@Entity
@DiscriminatorValue("ALUMNO")
class RolAlumno(
    usuario: Usuario,

    @OneToMany(mappedBy = "alumno", cascade = [CascadeType.ALL], orphanRemoval = true)
    var inscripciones: MutableList<Inscripcion> = mutableListOf()
) : Rol(usuario = usuario) {

    var puntos: Int = 0

    override fun puedeGestionarCurso(curso: Curso): Boolean = false

    override fun puedeRegistrarPago(inscripcion: Inscripcion): Boolean = false

    override fun puedeAsignarBeneficio(inscripcion: Inscripcion): Boolean = false

    fun inscribirEnCurso(inscripcion: Inscripcion) {
        inscripciones.add(inscripcion)
    }

    fun getInscripcionPorCurso(curso: Curso): Inscripcion {
        return inscripciones.find { it.curso.id == curso.id && it.fechaBaja == null }
            ?: throw NoSuchElementException("El alumno no está inscrito en el curso ${curso.nombre}")
    }

    fun getInscripcionesActivas(): List<Inscripcion> {
        return inscripciones.filter { it.fechaBaja == null }
    }

    fun estaAlDiaEnTodos(): Boolean {
        return getInscripcionesActivas().all { it.estaAlDia() }
    }

    fun calcularDeudaTotal(): BigDecimal {
        return getInscripcionesActivas()
            .map { it.calcularDeudaPendiente() }
            .fold(BigDecimal.ZERO) { acc, deuda -> acc + deuda }
    }
}