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
        name = "curso_dias",
        joinColumns = [JoinColumn(name = "curso_id")]
    )
    @Enumerated(EnumType.STRING)
    @Column(name = "dia")
    var dias: MutableList<DayOfWeek> = mutableListOf(),

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "curso_horarios", joinColumns = [JoinColumn(name = "curso_id")])
    @Column(name = "horario")
    val horarios: Set<LocalTime> = emptySet(),

    val arancel: Double,

    @Enumerated(EnumType.STRING)
    val tipoPago: PagoType,
    
    @Enumerated(EnumType.STRING)
    var estado: EstadoType = EstadoType.ACTIVO,

    @Column
    var fechaBaja: LocalDate
)

