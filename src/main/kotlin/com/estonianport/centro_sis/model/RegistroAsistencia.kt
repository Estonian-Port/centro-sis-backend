package com.estonianport.centro_sis.model

import jakarta.persistence.*

@Entity
@Table(
    name = "registro_asistencia",
    indexes = [
        Index(name = "idx_registro_parte", columnList = "parte_asistencia_id"),
        Index(name = "idx_registro_alumno", columnList = "alumno_id")
    ]
)
class RegistroAsistencia(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parte_asistencia_id", nullable = false)
    val parteAsistencia: ParteAsistencia,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "alumno_id", nullable = false)
    val alumno: RolAlumno,

    @Column(nullable = false)
    var presente: Boolean = false,
) {
}