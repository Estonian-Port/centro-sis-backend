package com.estonianport.centro_sis.model

import com.estonianport.centro_sis.model.enums.BeneficioType
import com.estonianport.centro_sis.model.enums.EstadoType
import com.estonianport.centro_sis.model.enums.RolType
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
    val dni: String,

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

    fun getRolTypes(): MutableSet<RolType> =
        listaRol.map {
            when (it) {
                is RolAdmin -> RolType.ADMINISTRADOR
                is RolProfesor -> RolType.PROFESOR
                is RolAlumno -> RolType.ALUMNO
                else -> throw IllegalArgumentException("Rol desconocido")
            }
        }.toMutableSet()

    fun asignarRol(rol: Rol) {
        listaRol.add(rol)
    }

    fun quitarRol(rol: Rol) {
        listaRol.remove(rol)
    }

    fun getRolAlumno(): RolAlumno {
        return listaRol.filterIsInstance<RolAlumno>().firstOrNull()
            ?: throw NoSuchElementException("El usuario no tiene el rol de alumno")
    }

    fun getRolProfesor(): RolProfesor {
        return listaRol.filterIsInstance<RolProfesor>().firstOrNull()
            ?: throw NoSuchElementException("El usuario no tiene el rol de profesor")
    }

    fun getRolAdmin(): RolAdmin {
        return listaRol.filterIsInstance<RolAdmin>().firstOrNull()
            ?: throw NoSuchElementException("El usuario no tiene el rol de administrador")
    }
}