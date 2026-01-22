package com.estonianport.centro_sis.model

import jakarta.persistence.*
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
@Table(
    name = "parte_asistencia",
    indexes = [
        Index(name = "idx_parte_curso_fecha", columnList = "curso_id, fecha")
    ],
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_curso_fecha",
            columnNames = ["curso_id", "fecha"]
        )
    ]
)
class ParteAsistencia(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "curso_id", nullable = false)
    val curso: Curso,

    @Column(nullable = false)
    val fecha: LocalDate = LocalDate.now(),

    @OneToMany(
        mappedBy = "parteAsistencia",
        cascade = [CascadeType.ALL],
        orphanRemoval = true,
        fetch = FetchType.LAZY
    )
    val registros: MutableList<RegistroAsistencia> = mutableListOf(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tomado_por_id", nullable = false)
    val tomadoPor: Usuario,

) {
    fun getTotalAlumnos(): Int = registros.size

    fun getTotalPresentes(): Int = registros.count { it.presente }

    fun getTotalAusentes(): Int = registros.count { !it.presente }

    fun getAlumnosPresentes(): List<Usuario> {
        return registros.filter { it.presente }.map { it.alumno.usuario }
    }

    fun getAlumnosAusentes(): List<Usuario> {
        return registros.filter { !it.presente }.map { it.alumno.usuario }
    }

    fun getPorcentajeAsistencia(): Double {
        if (registros.isEmpty()) return 0.0
        return (getTotalPresentes().toDouble() / getTotalAlumnos()) * 100
    }
}