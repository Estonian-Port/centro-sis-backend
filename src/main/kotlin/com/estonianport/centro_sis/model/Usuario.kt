package com.estonianport.unique.model

import com.estonianport.unique.dto.UsuarioAbmDTO
import com.estonianport.unique.model.enums.EstadoType
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

    @Column
    val esAdministrador: Boolean = false,

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

    fun toUsuarioAbmDto(): UsuarioAbmDTO {
        return UsuarioAbmDTO(id, nombre, apellido)
    }

    fun confirmarPrimerLogin() {
        if (ultimoIngreso != null) {
            estado = EstadoType.INACTIVO
        }
    }

    fun piscinaAsignada() {
        if (ultimoIngreso != null) {
            estado = EstadoType.ACTIVO // Pasa a ACTIVO cuando se le asigna una piscina y ya ingresó al sistema
        }
    }
}