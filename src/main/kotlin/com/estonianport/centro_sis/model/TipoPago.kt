package com.estonianport.centro_sis.model

import com.estonianport.centro_sis.model.enums.PagoType
import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated

@Embeddable
data class TipoPago(
    @Enumerated(EnumType.STRING)
    val tipoPago: PagoType,

    @Column(nullable = false)
    val monto: Double
)
