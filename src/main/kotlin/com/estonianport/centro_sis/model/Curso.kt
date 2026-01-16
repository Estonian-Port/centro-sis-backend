package com.estonianport.centro_sis.model

import com.estonianport.centro_sis.model.enums.CursoType
import com.estonianport.centro_sis.model.enums.PagoType
import com.estonianport.centro_sis.model.enums.EstadoCursoType
import com.estonianport.centro_sis.model.enums.EstadoType
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDate
import jakarta.persistence.Transient

@Entity
@Inheritance(strategy = InheritanceType.JOINED)
abstract class Curso(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false)
    val nombre: String,

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "curso_profesores",
        joinColumns = [JoinColumn(name = "curso_id")],
        inverseJoinColumns = [JoinColumn(name = "profesor_id")]
    )
    val profesores: MutableSet<RolProfesor> = mutableSetOf(),

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
        name = "curso_horarios",
        joinColumns = [JoinColumn(name = "curso_id")]
    )
    var horarios: MutableList<Horario> = mutableListOf(),

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
        name = "curso_tipos_pago",
        joinColumns = [JoinColumn(name = "curso_id")]
    )
    var tiposPago: MutableSet<TipoPago> = mutableSetOf(),

    @Column(nullable = false)
    var fechaInicio: LocalDate,

    @Column(nullable = false)
    var fechaFin: LocalDate,

    @Column
    var fechaBaja: LocalDate? = null,

    @Column(nullable = false, precision = 5, scale = 2)
    var recargoAtraso: BigDecimal = BigDecimal.ONE,

    @OneToMany(mappedBy = "curso")
    val inscripciones: MutableList<Inscripcion> = mutableListOf(),

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var estadoAlta : EstadoType = EstadoType.PENDIENTE,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val tipoCurso : CursoType,
) {

    @get:Transient
    val estado: EstadoCursoType
        get() = calcularEstado()

    private fun calcularEstado(): EstadoCursoType {
        val hoy = LocalDate.now()
        return when {
            hoy.isBefore(fechaInicio) -> EstadoCursoType.POR_COMENZAR
            hoy.isAfter(fechaFin) -> EstadoCursoType.FINALIZADO
            else -> EstadoCursoType.EN_CURSO
        }
    }

    init {
        require(fechaFin.isAfter(fechaInicio)) {
            "La fecha de fin debe ser posterior a la fecha de inicio"
        }
        require(recargoAtraso >= BigDecimal.ZERO) {
            "El recargo por atraso no puede ser negativo"
        }
    }

    fun definirPrecio(tipo: PagoType, monto: BigDecimal, cuotas: Int) {
        require(monto > BigDecimal.ZERO) {
            "El monto debe ser mayor a cero"
        }

        tiposPago.removeIf { it.tipo == tipo }
        tiposPago.add(TipoPago(tipo, monto, cuotas))
    }

    fun eliminarTipoPago(tipo: PagoType) {
        require(tiposPago.size > 1) {
            "El curso debe tener al menos un tipo de pago disponible"
        }
        tiposPago.removeIf { it.tipo == tipo }
    }

    fun getInscripcionesActivas(): List<Inscripcion> {
        return inscripciones.filter { it.fechaBaja == null }
    }

    fun calcularRecaudacionTotal(): BigDecimal {
        return inscripciones
            .flatMap { it.pagos }
            .filter { it.fechaBaja == null }
            .map { it.monto }
            .fold(BigDecimal.ZERO) { acc, monto -> acc + monto }
    }

    fun agregarProfesor(profesor: RolProfesor) {
        profesores.add(profesor)
        profesor.cursos.add(this)
    }

    fun removerProfesor(profesor: RolProfesor) {
        profesores.remove(profesor)
        profesor.cursos.remove(this)
    }

    abstract fun calcularPagoProfesor(): BigDecimal
}

@Entity
@Table(name = "cursos_alquiler")
class CursoAlquiler(
    id: Long = 0,
    nombre: String,
    profesores: MutableSet<RolProfesor> = mutableSetOf(),
    horarios: MutableList<Horario> = mutableListOf(),
    tiposPago: MutableSet<TipoPago> = mutableSetOf(),
    fechaInicio: LocalDate,
    fechaFin: LocalDate,
    fechaBaja: LocalDate? = null,
    recargoAtraso: BigDecimal = BigDecimal.ONE,
    tipoCurso: CursoType = CursoType.ALQUILER,
    inscripciones: MutableList<Inscripcion> = mutableListOf(),
    estadoAlta: EstadoType = EstadoType.PENDIENTE,

    @Column(nullable = false, precision = 10, scale = 2)
    var precioAlquiler: BigDecimal,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profesor_id")
    var profesor: RolProfesor? = null,

    @Column(name = "cuotas_alquiler")
    var cuotasAlquiler: Int = 1,

    @OneToMany(mappedBy = "curso", cascade = [CascadeType.ALL])
    val pagosAlquiler: MutableList<PagoAlquiler> = mutableListOf()
) : Curso(id, nombre, profesores, horarios, tiposPago, fechaInicio, fechaFin, fechaBaja, recargoAtraso, inscripciones, estadoAlta, tipoCurso) {

    init {
        require(precioAlquiler > BigDecimal.ZERO) {
            "El precio de alquiler debe ser mayor a cero"
        }
    }

    override fun calcularPagoProfesor(): BigDecimal {
        val recaudado = calcularRecaudacionTotal()
        return maxOf(BigDecimal.ZERO, recaudado - precioAlquiler)
    }

    fun registrarPagoAlquiler(): PagoAlquiler {
        val pago = PagoAlquiler(
            curso = this,
            monto = precioAlquiler,
            fecha = LocalDate.now(),
            profesor = profesor ?: throw IllegalStateException("El curso no tiene profesor asignado")
        )
        pagosAlquiler.add(pago)
        return pago
    }

    fun obtenerPagosActivos(): List<PagoAlquiler> {
        return pagosAlquiler.filter { it.estaActivo() }
    }

    fun calcularTotalPagado(): BigDecimal {
        return obtenerPagosActivos().sumOf { it.monto }
    }
}

@Entity
@Table(name = "cursos_comision")
class CursoComision(
    id: Long = 0,
    nombre: String,
    profesores: MutableSet<RolProfesor> = mutableSetOf(),
    horarios: MutableList<Horario> = mutableListOf(),
    tiposPago: MutableSet<TipoPago> = mutableSetOf(),
    fechaInicio: LocalDate,
    fechaFin: LocalDate,
    fechaBaja: LocalDate? = null,
    recargoAtraso: BigDecimal = BigDecimal.ONE,
    tipoCurso: CursoType = CursoType.COMISION,
    inscripciones: MutableList<Inscripcion> = mutableListOf(),
    estadoAlta: EstadoType = EstadoType.PENDIENTE,

    @Column(nullable = false, precision = 3, scale = 2)
    var porcentajeComision: BigDecimal = BigDecimal("0.50"),

    @OneToMany(mappedBy = "curso", cascade = [CascadeType.ALL])
    val pagosComision: MutableList<PagoComision> = mutableListOf(),

) : Curso(id, nombre, profesores, horarios, tiposPago, fechaInicio, fechaFin, fechaBaja, recargoAtraso, inscripciones, estadoAlta, tipoCurso) {

    init {
        require(porcentajeComision >= BigDecimal.ZERO && porcentajeComision <= BigDecimal.ONE) {
            "El porcentaje de comisión debe estar entre 0 y 1"
        }
    }

    override fun calcularPagoProfesor(): BigDecimal {
        val recaudado = calcularRecaudacionTotal()
        return recaudado * porcentajeComision
    }

    fun registrarPagoComision(profesor: RolProfesor): PagoComision {
        val pago = PagoComision(
            curso = this,
            monto = calcularPagoProfesor(),
            fecha = LocalDate.now(),
            profesor = profesor
        )
        pagosComision.add(pago)
        return pago
    }

    fun obtenerPagosActivos(): List<PagoComision> {
        return pagosComision.filter { it.estaActivo() }
    }
}