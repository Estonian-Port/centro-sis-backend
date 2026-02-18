package com.estonianport.centro_sis.model

import com.estonianport.centro_sis.model.enums.TipoPagoConcepto
import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.math.BigDecimal
import java.time.LocalDate
import java.time.ZoneId

@Entity
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "tipo_pago", discriminatorType = DiscriminatorType.STRING)
abstract class Pago(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false, precision = 10, scale = 2)
    val monto: BigDecimal,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "registrado_por_id", nullable = false)
    val registradoPor: Usuario,

    @Column(nullable = false)
    val fecha: LocalDate = LocalDate.now(),

    @Column
    var fechaBaja: LocalDate? = null,

    @Column(columnDefinition = "VARCHAR(500)")
    @JdbcTypeCode(SqlTypes.VARCHAR)
    var observaciones: String? = null
) {
    @get:Transient // NO guardar en BD - solo en código
    abstract val tipo: TipoPagoConcepto

    fun anular(motivo: String, anulador: Usuario) {
        this.fechaBaja = LocalDate.now()
        this.observaciones = "ANULADO por ${anulador.nombreCompleto()} - Motivo: $motivo"
    }

    fun estaActivo(): Boolean = fechaBaja == null

    fun esDelMes(mes: Int, anio: Int): Boolean {
        return fecha.monthValue == mes && fecha.year == anio
    }
}

// ========================================
// Pago de alumno por inscripción
// ========================================
@Entity
@DiscriminatorValue("CURSO")
@Table(name = "pago_curso")
class PagoCurso(
    monto: BigDecimal,
    registradoPor: Usuario,
    fecha: LocalDate = LocalDate.now(ZoneId.of("America/Argentina/Buenos_Aires")),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inscripcion_id", nullable = false)
    val inscripcion: Inscripcion,

    @Column(nullable = false)
    val conRecargo: Boolean = false,

    @Column(nullable = false)
    val beneficioAplicado: Int = 0

) : Pago(
    monto = monto,
    registradoPor = registradoPor,
    fecha = fecha
) {
    // NO guardar en BD - solo código
    @get:Transient
    override val tipo: TipoPagoConcepto
        get() = TipoPagoConcepto.CURSO

    fun getAlumno(): RolAlumno = inscripcion.alumno
    fun getCurso(): Curso = inscripcion.curso

    fun calcularDescuentoAplicado(): BigDecimal {
        return inscripcion.tipoPagoSeleccionado.monto *
                (BigDecimal(beneficioAplicado) / BigDecimal(100))
    }

    fun calcularRecargoAplicado(): BigDecimal {
        if (!conRecargo) return BigDecimal.ZERO

        val montoBase = inscripcion.calcularMontoPorCuota()
        val montoConRecargo = montoBase * inscripcion.curso.recargoAtraso
        return montoConRecargo - montoBase
    }
}

// ========================================
// Pago de profesor al instituto (alquiler)
// ========================================
@Entity
@DiscriminatorValue("ALQUILER")
@Table(name = "pago_alquiler")
class PagoAlquiler(
    monto: BigDecimal,
    registradoPor: Usuario,
    fecha: LocalDate = LocalDate.now(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "curso_id", nullable = false)
    val curso: CursoAlquiler,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profesor_id", nullable = false)
    val profesor: RolProfesor,

    @Column
    val mesPago: Int? = fecha.monthValue,

    @Column
    val anioPago: Int? = fecha.year

) : Pago(
    monto = monto,
    registradoPor = registradoPor,
    fecha = fecha
) {
    // NO guardar en BD - solo código
    @get:Transient
    override val tipo: TipoPagoConcepto
        get() = TipoPagoConcepto.ALQUILER

    fun esDelPeriodo(mes: Int, anio: Int): Boolean {
        return mesPago == mes && anioPago == anio
    }
}

// ========================================
// Pago del instituto a profesor (comisión)
// ========================================
@Entity
@DiscriminatorValue("COMISION")
@Table(name = "pago_comision")
class PagoComision(
    monto: BigDecimal,
    registradoPor: Usuario,
    fecha: LocalDate = LocalDate.now(),


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "curso_id", nullable = false)
    val curso: CursoComision,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profesor_id", nullable = false)
    val profesor: RolProfesor,

    @Column
    val mesPago: Int? = fecha.monthValue,

    @Column
    val anioPago: Int? = fecha.year

) : Pago(
    monto = monto,
    registradoPor = registradoPor,
    fecha = fecha
) {
    // NO guardar en BD - solo código
    @get:Transient
    override val tipo: TipoPagoConcepto
        get() = TipoPagoConcepto.COMISION

    fun esDelPeriodo(mes: Int, anio: Int): Boolean {
        return mesPago == mes && anioPago == anio
    }
}