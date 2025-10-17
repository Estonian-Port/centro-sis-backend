package com.estonianport.centro_sis.model

import com.estonianport.centro_sis.model.enums.EstadoType
import com.estonianport.centro_sis.model.enums.PagoType
import jakarta.persistence.*
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

@Entity
@Table(name = "cursos")
data class Curso(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column
    val nombre: String,

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "curso_dias", joinColumns = [JoinColumn(name = "curso_id")])
    @Column(name = "dia")
    val dias: Set<DayOfWeek> = emptySet(),

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "curso_horarios", joinColumns = [JoinColumn(name = "curso_id")])
    @Column(name = "horario")
    val horarios: Set<LocalTime> = emptySet(),

    val arancel: Double,

    @Enumerated(EnumType.STRING)
    val tipoPago: PagoType,

    @ManyToOne
    @JoinColumn(name = "profesor_id")
    var profesorAsignado: Usuario? = null,

    @ManyToMany(mappedBy = "cursosActivos")
    val alumnosInscriptosActivos: Set<Usuario> = emptySet(),

    @ManyToMany(mappedBy = "cursosDadosDeBaja")
    val alumnosInscriptosBaja: Set<Usuario> = emptySet(),

    @Enumerated(EnumType.STRING)
    var estado: EstadoType = EstadoType.ACTIVO,

    @Column
    var fechaBaja: LocalDate
)

