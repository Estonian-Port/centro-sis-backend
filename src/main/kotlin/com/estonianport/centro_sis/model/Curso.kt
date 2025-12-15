package com.estonianport.centro_sis.model

import com.estonianport.centro_sis.model.enums.PagoType
import com.estonianport.centro_sis.model.enums.EstadoCursoType
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDate

@Entity
@Inheritance(strategy = InheritanceType.JOINED)
abstract class Curso(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false)
    val nombre: String,

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
        name = "curso_horarios",
        joinColumns = [JoinColumn(name = "curso_id")]
    )
    val horarios: MutableList<Horario> = mutableListOf(),

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
        name = "curso_tipos_pago",
        joinColumns = [JoinColumn(name = "curso_id")]
    )
    val tiposPago: MutableSet<TipoPago> = mutableSetOf(),

    @Column(nullable = false)
    var fechaInicio: LocalDate,

    @Column(nullable = false)
    var fechaFin: LocalDate,

    @Column(nullable = false, precision = 5, scale = 2)
    var recargoAtraso: BigDecimal = BigDecimal.ONE,

    @OneToMany(mappedBy = "curso")
    val inscripciones: MutableList<Inscripcion> = mutableListOf()
) {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var estado: EstadoCursoType = EstadoCursoType.POR_COMENZAR

    init {
        require(fechaFin.isAfter(fechaInicio)) {
            "La fecha de fin debe ser posterior a la fecha de inicio"
        }
        require(recargoAtraso >= BigDecimal.ZERO) {
            "El recargo por atraso no puede ser negativo"
        }
    }

    fun definirPrecio(tipo: PagoType, monto: BigDecimal) {
        require(monto > BigDecimal.ZERO) {
            "El monto debe ser mayor a cero"
        }

        tiposPago.removeIf { it.tipoPago == tipo }
        tiposPago.add(TipoPago(tipo, monto))
    }

    fun eliminarTipoPago(tipo: PagoType) {
        require(tiposPago.size > 1) {
            "El curso debe tener al menos un tipo de pago disponible"
        }
        tiposPago.removeIf { it.tipoPago == tipo }
    }

    fun actualizarEstado() {
        val hoy = LocalDate.now()
        estado = when {
            hoy.isBefore(fechaInicio) -> EstadoCursoType.POR_COMENZAR
            hoy.isAfter(fechaFin) -> EstadoCursoType.FINALIZADO
            else -> EstadoCursoType.EN_CURSO
        }
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

    abstract fun calcularPagoProfesor(): BigDecimal
}

@Entity
@Table(name = "cursos_alquiler")
class CursoAlquiler(
    id: Long = 0,
    nombre: String,
    horarios: MutableList<Horario> = mutableListOf(),
    tiposPago: MutableSet<TipoPago> = mutableSetOf(),
    fechaInicio: LocalDate,
    fechaFin: LocalDate,
    recargoAtraso: BigDecimal = BigDecimal.ONE,

    @Column(nullable = false, precision = 10, scale = 2)
    var precioAlquiler: BigDecimal,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profesor_id")
    var profesor: RolProfesor? = null
) : Curso(id, nombre, horarios, tiposPago, fechaInicio, fechaFin, recargoAtraso) {

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
        return PagoAlquiler(
            curso = this,
            monto = precioAlquiler,
            fecha = LocalDate.now(),
            profesor = profesor ?: throw IllegalStateException("El curso no tiene profesor asignado")
        )
    }
}

@Entity
@Table(name = "cursos_comision")
class CursoComision(
    id: Long = 0,
    nombre: String,
    horarios: MutableList<Horario> = mutableListOf(),
    tiposPago: MutableSet<TipoPago> = mutableSetOf(),
    fechaInicio: LocalDate,
    fechaFin: LocalDate,
    recargoAtraso: BigDecimal = BigDecimal.ONE,

    @Column(nullable = false, precision = 3, scale = 2)
    var porcentajeComision: BigDecimal = BigDecimal("0.50")
) : Curso(id, nombre, horarios, tiposPago, fechaInicio, fechaFin, recargoAtraso) {

    init {
        require(porcentajeComision >= BigDecimal.ZERO && porcentajeComision <= BigDecimal.ONE) {
            "El porcentaje de comisión debe estar entre 0 y 1"
        }
    }

    override fun calcularPagoProfesor(): BigDecimal {
        val recaudado = calcularRecaudacionTotal()
        return recaudado * porcentajeComision
    }
}