package com.estonianport.centro_sis.model

import com.estonianport.centro_sis.model.enums.EstadoType
import com.estonianport.centro_sis.model.enums.PagoType
import com.fasterxml.jackson.annotation.JsonIgnore
import jakarta.persistence.*
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

@Entity
data class Curso(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column
    val nombre: String,

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
        name = "curso_horarios",
        joinColumns = [JoinColumn(name = "curso_id")]
    )
    val horarios: MutableList<Horario> = mutableListOf(),

    val arancel: Double,

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
        name = "curso_tipos_pago",
        joinColumns = [JoinColumn(name = "curso_id")]
    )
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_pago")
    val tiposPago: MutableSet<PagoType> = mutableSetOf(),

    @Enumerated(EnumType.STRING)
    var estado: EstadoType = EstadoType.ACTIVO,

    @Column
    var fechaBaja: LocalDate? = null,
)

