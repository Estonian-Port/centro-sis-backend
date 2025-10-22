package com.estonianport.centro_sis.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import java.time.LocalDate

@Entity
class Pago(
        @Id @GeneratedValue(strategy = GenerationType.IDENTITY) val id: Long = 0,
        @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(nullable = false) var alumno: RolAlumno,
        @Column(nullable = false) var monto: Double,
        @Column(nullable = false) var fecha: LocalDate = LocalDate.now(),
        @Column(nullable = false) val retraso: Boolean = false
)
