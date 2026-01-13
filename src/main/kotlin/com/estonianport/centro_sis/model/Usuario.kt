package com.estonianport.centro_sis.model

import com.estonianport.centro_sis.model.enums.EstadoType
import com.estonianport.centro_sis.model.enums.RolType
import jakarta.persistence.*
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
@Table(name = "usuario")
class Usuario(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false)
    var nombre: String,

    @Column(nullable = false)
    var apellido: String,

    @Column(nullable = false)
    var celular: Long,

    @Column(nullable = false, unique = true)
    var email: String,

    @Column(nullable = false)
    var dni: String,

    @OneToMany(mappedBy = "usuario", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    var listaRol: MutableSet<Rol> = mutableSetOf(),

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var estado: EstadoType = EstadoType.PENDIENTE,

    @Column(nullable = false)
    var fechaNacimiento: LocalDate,

    @Embedded
    var adultoResponsable: AdultoResponsable? = null

) {

    @Column
    var password: String? = null

    @Column(nullable = false)
    var fechaAlta: LocalDate = LocalDate.now()

    @Column
    var fechaBaja: LocalDate? = null

    @Column
    var ultimoIngreso: LocalDateTime? = null

    fun registrarIngreso() {
        ultimoIngreso = LocalDateTime.now()
    }

    fun confirmarPrimerLogin() {
        registrarIngreso()
        if (ultimoIngreso != null) {
            estado = EstadoType.INACTIVO
        }
    }

    fun esPrimerLogin(): Boolean {
        return estado == EstadoType.PENDIENTE
    }

    fun esMenorDeEdad(): Boolean {
        return fechaNacimiento.plusYears(18).isAfter(LocalDate.now())
    }

    fun nombreCompleto(): String = "$nombre $apellido"

    fun getRolTypes(): Set<RolType> =
        listaRol.map {
            when (it) {
                is RolAdmin -> RolType.ADMINISTRADOR
                is RolOficina -> RolType.OFICINA
                is RolProfesor -> RolType.PROFESOR
                is RolAlumno -> RolType.ALUMNO
                else -> throw IllegalArgumentException("Rol desconocido")
            }
        }.toSet()

    fun asignarRol(rol: Rol) {
        listaRol.add(rol)
    }

    fun quitarRol(rol: Rol) {
        rol.fechaBaja = LocalDate.now()
    }

    fun tieneRol(rolType: RolType): Boolean {
        return getRolTypes().contains(rolType)
    }

    fun getRolAlumno(): RolAlumno {
        return listaRol
            .filterIsInstance<RolAlumno>()
            .firstOrNull { it.fechaBaja == null }
            ?: throw NoSuchElementException("El usuario no tiene el rol de alumno activo")
    }

    fun getRolProfesor(): RolProfesor {
        return listaRol
            .filterIsInstance<RolProfesor>()
            .firstOrNull { it.fechaBaja == null }
            ?: throw NoSuchElementException("El usuario no tiene el rol de profesor activo")
    }

    fun getRolAdmin(): RolAdmin {
        return listaRol
            .filterIsInstance<RolAdmin>()
            .firstOrNull { it.fechaBaja == null }
            ?: throw NoSuchElementException("El usuario no tiene el rol de administrador activo")
    }

    fun getRolOficina(): RolOficina {
        return listaRol
            .filterIsInstance<RolOficina>()
            .firstOrNull { it.fechaBaja == null }
            ?: throw NoSuchElementException("El usuario no tiene el rol de oficina activo")
    }

    fun darDeBaja(fecha: LocalDate = LocalDate.now()) {
        this.fechaBaja = fecha
        this.estado = EstadoType.BAJA
        listaRol.forEach { it.fechaBaja = fecha }
    }

    fun reactivar() {
        require(estado == EstadoType.BAJA) {
            "Solo se pueden reactivar usuarios dados de baja"
        }
        this.fechaBaja = null
        this.estado = EstadoType.ACTIVO
    }
}