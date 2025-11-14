package com.estonianport.centro_sis.model

import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import java.time.DayOfWeek
import java.time.LocalTime

@Embeddable
data class Horario(
    @Enumerated(EnumType.STRING)
    @Column(name = "dia_semana")
    val dia: DayOfWeek,

    @Column(name = "hora_inicio")
    val horaInicio: LocalTime,

    @Column(name = "hora_fin")
    val horaFin: LocalTime
) {
    init {
        require(horaFin.isAfter(horaInicio)) {
            "La hora de fin debe ser posterior a la hora de inicio"
        }
    }
}