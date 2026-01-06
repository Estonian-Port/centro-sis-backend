package com.estonianport.centro_sis.model

import com.estonianport.centro_sis.model.enums.PagoType
import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import java.math.BigDecimal

@Embeddable
class TipoPago(

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val tipo: PagoType,

    @Column(nullable = false)
    val monto: BigDecimal
) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TipoPago) return false
        return tipo == other.tipo
    }

    override fun hashCode(): Int {
        return tipo.hashCode()
    }
}

