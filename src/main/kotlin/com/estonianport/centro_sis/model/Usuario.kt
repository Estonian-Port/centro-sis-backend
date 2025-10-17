package com.estonianport.centro_sis.model

import com.estonianport.centro_sis.model.enums.BeneficioType
import com.estonianport.centro_sis.model.enums.EstadoType
import jakarta.persistence.*
import java.time.LocalDate

@Entity
class Usuario(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long,

    @Column
    var nombre: String,

    @Column
    var apellido: String,

    @Column
    var celular: Long,

    @Column
    var email: String,

    @OneToMany(mappedBy = "usuario", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    var listaRol: MutableSet<Rol> = mutableSetOf(),

    @Enumerated(EnumType.STRING)
    var estado: EstadoType = EstadoType.PENDIENTE,
) {

    @Column
    var password: String? = null

    @Column
    var fechaAlta: LocalDate = LocalDate.now()

    @Column
    var fechaBaja: LocalDate? = null

    @Column
    var ultimoIngreso: LocalDate? = null

    @ElementCollection(targetClass = BeneficioType::class, fetch = FetchType.EAGER)
    @CollectionTable(
        name = "usuario_beneficios",
        joinColumns = [JoinColumn(name = "usuario_id")]
    )
    @Enumerated(EnumType.STRING)
    val beneficios: MutableSet<BeneficioType> = mutableSetOf()

    fun confirmarPrimerLogin() {
        if (ultimoIngreso != null) {
            estado = EstadoType.INACTIVO
        }
    }
}