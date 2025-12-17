package com.estonianport.centro_sis.model

import com.estonianport.centro_sis.model.enums.EstadoCursoType
import com.estonianport.centro_sis.model.enums.EstadoType
import com.estonianport.centro_sis.model.enums.PagoType
import com.fasterxml.jackson.annotation.JsonIgnore
import jakarta.persistence.*
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
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

    @Column
    var fechaBaja: LocalDate? = null,

    @Column
    var fechaInicio: LocalDate,

    @Column
    var fechaFin: LocalDate,
) {
    @Enumerated(EnumType.STRING)
    var estado: EstadoCursoType = actualizarEstadoCurso()

    fun actualizarEstadoCurso(): EstadoCursoType {
        val hoy = LocalDate.now()
        return when {
            hoy.isBefore(fechaInicio) -> EstadoCursoType.POR_COMENZAR
            hoy.isAfter(fechaFin) -> EstadoCursoType.FINALIZADO
            else -> EstadoCursoType.EN_CURSO
        }
    }
}

