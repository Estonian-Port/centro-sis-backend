package com.estonianport.centro_sis.model

import jakarta.persistence.*

@Entity
@Table(name = "adulto_responsable")
class AdultoResponsable(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false)
    var nombre: String,

    @Column(nullable = false)
    var apellido: String,

    @Column(nullable = false)
    var dni: String,

    @Column(nullable = false)
    var celular: Long,

    @Column(nullable = false)
    var relacion: String,

    @OneToOne(mappedBy = "adultoResponsable")
    var alumnoMenor: Usuario? = null
) {
    fun nombreCompleto(): String = "$nombre $apellido"
}