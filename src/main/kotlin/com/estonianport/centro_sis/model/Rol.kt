package com.estonianport.centro_sis.model

import com.estonianport.centro_sis.model.enums.RolType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.ManyToOne
import jakarta.persistence.PrimaryKeyJoinColumn
import java.time.LocalDate

@Entity
class Rol(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column
    @Enumerated(EnumType.STRING)
    val rol: RolType,

    @ManyToOne
    @PrimaryKeyJoinColumn
    var usuario: Usuario,

    @ManyToOne
    @PrimaryKeyJoinColumn
    var curso: Curso){

    @Column
    var fechaAlta: LocalDate = LocalDate.now()

    @Column
    var fechaBaja: LocalDate? = null
}