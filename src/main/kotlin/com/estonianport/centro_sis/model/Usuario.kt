package com.estonianport.centro_sis.model

import com.estonianport.centro_sis.model.enums.EstadoType
import com.estonianport.centro_sis.model.enums.RolType
import com.estonianport.centro_sis.model.enums.TipoAcceso
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

    @Column(nullable = false, columnDefinition = "VARCHAR(255)")
    @JdbcTypeCode(SqlTypes.VARCHAR)
    var nombre: String,

    @Column(nullable = false, columnDefinition = "VARCHAR(255)")
    @JdbcTypeCode(SqlTypes.VARCHAR)
    var apellido: String,

    @Column(nullable = false)
    var celular: Long,

    @Column(nullable = false, unique = true, columnDefinition = "VARCHAR(255)")
    @JdbcTypeCode(SqlTypes.VARCHAR)
    var email: String,

    @Column(nullable = false, columnDefinition = "VARCHAR(50)")
    @JdbcTypeCode(SqlTypes.VARCHAR)
    var dni: String,

    @OneToMany(mappedBy = "usuario", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    var listaRol: MutableSet<Rol> = mutableSetOf(),

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var estado: EstadoType = EstadoType.PENDIENTE,

    @Column(nullable = false)
    var fechaNacimiento: LocalDate,

    @OneToOne(cascade = [CascadeType.ALL], fetch = FetchType.LAZY, orphanRemoval = true)
    @JoinColumn(name = "adulto_responsable_id", referencedColumnName = "id")
    var adultoResponsable: AdultoResponsable? = null,

    @OneToMany(
        mappedBy = "usuario",
        cascade = [CascadeType.ALL],
        fetch = FetchType.LAZY,
        orphanRemoval = true
    )
    val accesos: MutableList<Acceso> = mutableListOf()

) {

    @Column(columnDefinition = "VARCHAR(255)")
    @JdbcTypeCode(SqlTypes.VARCHAR)
    var password: String? = null

    @Column(nullable = false)
    var fechaAlta: LocalDate = LocalDate.now()

    @Column
    var fechaBaja: LocalDate? = null

    @Column
    var ultimoIngresoAlSistema: LocalDateTime? = null

    fun registrarIngresoAlSistema() {
        ultimoIngresoAlSistema = LocalDateTime.now()
    }

    fun confirmarPrimerLogin() {
        registrarIngresoAlSistema()
        if (ultimoIngresoAlSistema != null) {
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
        rolAEliminar.fechaBaja = LocalDate.now()
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

    fun getRolPorteria(): RolPorteria {
        return listaRol
            .filterIsInstance<RolPorteria>()
            .firstOrNull { it.fechaBaja == null }
            ?: throw NoSuchElementException("El usuario no tiene el rol de portería activo")
    }

    fun esPorteria(): Boolean {
        return listaRol.any { it is RolPorteria && estado === EstadoType.ACTIVO }
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

    fun asignarAdultoResponsable(adulto: AdultoResponsable) {
        require(esMenorDeEdad()) {
            "Solo se puede asignar adulto responsable a menores de edad"
        }
        this.adultoResponsable = adulto
        adulto.alumnoMenor = this
    }

    fun quitarAdultoResponsable() {
        this.adultoResponsable?.alumnoMenor = null
        this.adultoResponsable = null
    }

    fun validarAcceso() {
        val hoy = LocalDate.now()
        if (tuvoAccesoEnFecha(hoy)) {
            throw IllegalStateException("El usuario ya ha registrado un acceso hoy")
        }
    }

    fun registrarAcceso(
        tipoAcceso: TipoAcceso,
    ): Acceso {
        validarAcceso()

        val nuevoAcceso = Acceso(
            usuario = this,
            tipoAcceso = tipoAcceso,
        )

        accesos.add(nuevoAcceso)
        return nuevoAcceso
    }

    fun tuvoAccesoEnFecha(fecha: LocalDate): Boolean {
        return accesos.any { it.esDeFecha(fecha) }
    }

    fun getAccesosDelMes(mes: Int, anio: Int): List<Acceso> {
        return accesos.filter {
            it.fechaHora.monthValue == mes && it.fechaHora.year == anio
        }
    }

    fun getUltimoAcceso(): Acceso? {
        return accesos.maxByOrNull { it.fechaHora }
    }

    fun contarAccesosEnRango(desde: LocalDate, hasta: LocalDate): Int {
        return accesos.count { acceso ->
            val fecha = acceso.fechaHora.toLocalDate()
            !fecha.isBefore(desde) && !fecha.isAfter(hasta)
        }
    }
}