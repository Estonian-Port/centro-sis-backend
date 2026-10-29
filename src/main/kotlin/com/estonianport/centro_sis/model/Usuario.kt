package com.estonianport.centro_sis.model

import com.estonianport.centro_sis.common.AppTime
import com.estonianport.centro_sis.model.enums.EstadoType
import com.estonianport.centro_sis.model.enums.RolType
import com.estonianport.centro_sis.model.enums.TipoAcceso
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonManagedReference
import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
@Table(name = "usuario")
class Usuario(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    var nombre: String,

    @Column(nullable = false)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    var apellido: String,

    @Column(nullable = false)
    var celular: Long,

    @Column(nullable = false, unique = true)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    var email: String,

    @Column(nullable = false)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    var dni: String,

    @OneToMany(mappedBy = "usuario", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    @JsonIgnore
    var listaRol: MutableSet<Rol> = mutableSetOf(),

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var estado: EstadoType = EstadoType.PENDIENTE,

    @Column(nullable = false)
    var fechaNacimiento: LocalDate,

    @OneToOne(cascade = [CascadeType.ALL], fetch = FetchType.EAGER, orphanRemoval = true)
    @JoinColumn(name = "adulto_responsable_id", referencedColumnName = "id")
    var adultoResponsable: AdultoResponsable? = null,

    @OneToMany(
        mappedBy = "usuario",
        cascade = [CascadeType.ALL],
        fetch = FetchType.LAZY,
        orphanRemoval = true
    )
    @JsonIgnore
    val accesos: MutableList<Acceso> = mutableListOf()

) {

    @Column
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @JsonIgnore
    var password: String? = null

    @Column(nullable = false)
    var fechaAlta: LocalDate = AppTime.hoy()

    @Column
    var fechaBaja: LocalDate? = null

    @Column
    var ultimoIngresoAlSistema: LocalDateTime? = null

    fun registrarIngresoAlSistema() {
        ultimoIngresoAlSistema = AppTime.ahora()
    }

    fun confirmarPrimerLogin() {
        registrarIngresoAlSistema()
        if (ultimoIngresoAlSistema != null) {
            estado = EstadoType.INACTIVO
        }
        // Si tiene un rol distinto a alumno o profesor, pasa a activo al logearse
        if(!tieneRol(RolType.ALUMNO) || !tieneRol(RolType.PROFESOR)){
            estado = EstadoType.ACTIVO
        }
    }

    fun esPrimerLogin(): Boolean {
        return estado == EstadoType.PENDIENTE
    }

    fun nombreCompleto(): String = "$nombre $apellido"

    fun getRolTypes(): Set<RolType> =
        listaRol.filter { it.fechaBaja == null }
            .map {
            when (it) {
                is RolAdmin -> RolType.ADMINISTRADOR
                is RolOficina -> RolType.OFICINA
                is RolProfesor -> RolType.PROFESOR
                is RolAlumno -> RolType.ALUMNO
                is RolPorteria -> RolType.PORTERIA
                else -> throw IllegalArgumentException("Rol desconocido")
            }
        }.toSet()

    fun asignarRol(rol: Rol) {
        listaRol.add(rol)
    }

    fun quitarRol(rol: RolType) {
        if (!tieneRol(rol)) {
            throw NoSuchElementException("El usuario no tiene el rol $rol")
        }
        val rolAEliminar = listaRol.first {
            when (rol) {
                RolType.ADMINISTRADOR -> it is RolAdmin
                RolType.OFICINA -> it is RolOficina
                RolType.PROFESOR -> it is RolProfesor
                RolType.ALUMNO -> it is RolAlumno
                RolType.PORTERIA -> it is RolPorteria
            }
        }
        rolAEliminar.fechaBaja = AppTime.hoy()
        listaRol.remove(rolAEliminar)
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

    fun getRolPorteria(): RolPorteria {
        return listaRol
            .filterIsInstance<RolPorteria>()
            .firstOrNull { it.fechaBaja == null }
            ?: throw NoSuchElementException("El usuario no tiene el rol de portería activo")
    }
}