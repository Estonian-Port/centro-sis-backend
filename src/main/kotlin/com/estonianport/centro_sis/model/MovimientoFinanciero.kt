package com.estonianport.centro_sis.model

import com.estonianport.centro_sis.model.enums.ConceptoMovimiento
import com.estonianport.centro_sis.model.enums.TipoMovimiento
import java.math.BigDecimal
import java.time.LocalDate
import jakarta.persistence.*

@Entity
@Table(name = "movimientos_financieros")
class MovimientoFinanciero(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val tipo: TipoMovimiento, // INGRESO, EGRESO

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val concepto: ConceptoMovimiento, // PAGO_ALUMNO, PAGO_ALQUILER_PROFESOR, PAGO_COMISION_PROFESOR, GASTO_OPERATIVO, etc.

    @Column(nullable = false, precision = 10, scale = 2)
    val monto: BigDecimal,

    @Column(nullable = false)
    val fecha: LocalDate = LocalDate.now(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_involucrado_id")
    val usuarioInvolucrado: Usuario?, // Alumno o Profesor según el caso

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "curso_id")
    val curso: Curso?, // Curso relacionado si aplica

    // Referencias polimórficas a los pagos originales
    @Column(name = "pago_id")
    val pagoId: Long? = null, // ID del Pago de alumno

    @Column(name = "pago_alquiler_id")
    val pagoAlquilerId: Long? = null, // ID del PagoAlquiler

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "registrado_por_id", nullable = false)
    val registradoPor: Usuario, // Quien registró el movimiento

    @Column(length = 500)
    var observaciones: String? = null,

    @Column
    var fechaAnulacion: LocalDate? = null
) {
    init {
        require(monto > BigDecimal.ZERO) {
            "El monto debe ser mayor a cero"
        }
    }

    fun anular(motivo: String) {
        this.fechaAnulacion = LocalDate.now()
        this.observaciones = "ANULADO: $motivo"
    }

    fun estaActivo(): Boolean = fechaAnulacion == null
}