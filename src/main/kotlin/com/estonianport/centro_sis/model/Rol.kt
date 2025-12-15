package com.estonianport.centro_sis.model

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

    @ManyToOne(fetch = FetchType.LAZY)
    var curso: Curso? = null
) : Rol(usuario = usuario) {

    override fun puedeGestionarCurso(curso: Curso): Boolean {
        // Solo puede gestionar cursos de alquiler donde él es el profesor
        return curso is CursoAlquiler && this.curso?.id == curso.id
    }

    override fun puedeRegistrarPago(inscripcion: Inscripcion): Boolean {
        // Solo puede registrar pagos en sus cursos de alquiler
        return inscripcion.curso is CursoAlquiler &&
                this.curso?.id == inscripcion.curso.id
    }

    override fun puedeAsignarBeneficio(inscripcion: Inscripcion): Boolean {
        // Solo puede asignar beneficios en sus cursos de alquiler
        return inscripcion.curso is CursoAlquiler &&
                this.curso?.id == inscripcion.curso.id
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

    fun inscribirEnCurso(curso: Curso, tipoPago: PagoType, beneficio: BigDecimal = BigDecimal.ONE): Inscripcion {
        // Verificar que el curso acepte ese tipo de pago
        val tipoPagoDisponible = curso.tiposPago.firstOrNull { it.tipoPago == tipoPago }
            ?: throw IllegalArgumentException("El curso ${curso.nombre} no acepta el tipo de pago $tipoPago")

        val inscripcion = Inscripcion(
            alumno = this,
            curso = curso,
            tipoPago = tipoPagoDisponible,
            beneficio = beneficio
        )

        inscripciones.add(inscripcion)
        return inscripcion
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