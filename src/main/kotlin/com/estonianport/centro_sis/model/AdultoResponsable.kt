package com.estonianport.centro_sis.model

import jakarta.persistence.*

@Embeddable
data class AdultoResponsable(
    @Column(name = "responsable_nombre")
    var nombre: String,

    @Column(name = "responsable_apellido")
    var apellido: String,

    @Column(name = "responsable_dni")
    var dni: String,

    @Column(name = "responsable_celular")
    var celular: Long,

    @Column(name = "responsable_relacion")
    var relacion: String
)