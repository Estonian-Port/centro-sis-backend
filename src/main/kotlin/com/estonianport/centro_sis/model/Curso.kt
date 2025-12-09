package com.estonianport.centro_sis.model

import com.estonianport.centro_sis.model.enums.PagoType
import jakarta.persistence.*
import java.time.LocalDate

@Entity
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "tipo_curso")
sealed class Curso(
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
    @Enumerated(EnumType.STRING)
    val tiposPago: MutableSet<TipoPago> = mutableSetOf(),

    @Column
    var fechaInicio: LocalDate,

    @Column
    var fechaFin: LocalDate
) {
    abstract fun definirPrecio(tipo: PagoType, monto: Double)
    abstract fun calcularPagoProfesor(recaudado: Double): Double
}

@Entity
@DiscriminatorValue("ALQUILER")
class CursoAlquiler(
    id: Long,
    nombre: String,
    horarios: MutableList<Horario>,
    tiposPago: MutableSet<TipoPago>,
    fechaInicio: LocalDate,
    fechaFin: LocalDate,
    val precioAlquiler: Double
) : Curso(id, nombre, horarios, tiposPago, fechaInicio, fechaFin) {

    override fun definirPrecio(tipo: PagoType, monto: Double) {
        tiposPago.removeIf { it.tipoPago == tipo }
        tiposPago.add(TipoPago(tipo, monto))
    }

    override fun calcularPagoProfesor(recaudado: Double): Double {
        return precioAlquiler
    }
}

@Entity
@DiscriminatorValue("COMISION")
class CursoComision(
    id: Long,
    nombre: String,
    horarios: MutableList<Horario>,
    tiposPago: MutableSet<TipoPago>,
    fechaInicio: LocalDate,
    fechaFin: LocalDate,
    val porcentajeComision: Double = 0.5
) : Curso(id, nombre, horarios, tiposPago, fechaInicio, fechaFin) {

    override fun definirPrecio(tipo: PagoType, monto: Double) {
        throw IllegalStateException("En cursos por comisión solo el instituto puede modificar precios")
    }

    override fun calcularPagoProfesor(recaudado: Double): Double {
        return recaudado * porcentajeComision
    }
}

