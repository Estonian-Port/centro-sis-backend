package com.estonianport.centro_sis.model

import com.estonianport.centro_sis.model.enums.EstadoType
import com.estonianport.centro_sis.model.enums.PagoType
import com.estonianport.centro_sis.model.enums.RolType
import com.estonianport.centro_sis.model.enums.TipoAcceso
import com.fasterxml.jackson.annotation.JsonIgnore
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDate

@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "rol_type", discriminatorType = DiscriminatorType.STRING)
abstract class Rol(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnore
    var usuario: Usuario,

    @Column
    var fechaAlta: LocalDate = LocalDate.now(),

    @Column
    var fechaBaja: LocalDate? = null
) {
    abstract fun puedeGestionarCurso(curso: Curso): Boolean
    abstract fun puedeRegistrarPago(inscripcion: Inscripcion): Boolean
    abstract fun puedeAsignarBeneficio(inscripcion: Inscripcion): Boolean
}

@Entity
@DiscriminatorValue("ADMINISTRADOR")
class RolAdmin(
    usuario: Usuario
) : Rol(usuario = usuario) {

    override fun puedeGestionarCurso(curso: Curso): Boolean = true

    override fun puedeRegistrarPago(inscripcion: Inscripcion): Boolean = true

    override fun puedeAsignarBeneficio(inscripcion: Inscripcion): Boolean = true
}

@Entity
@DiscriminatorValue("OFICINA")
class RolOficina(
    usuario: Usuario
) : Rol(usuario = usuario) {

    override fun puedeGestionarCurso(curso: Curso): Boolean = true

    override fun puedeRegistrarPago(inscripcion: Inscripcion): Boolean = true

    override fun puedeAsignarBeneficio(inscripcion: Inscripcion): Boolean {
        // Solo puede asignar beneficios en cursos por comisión
        return inscripcion.curso is CursoComision
    }
}

@Entity
@DiscriminatorValue("PROFESOR")
class RolProfesor(
    usuario: Usuario,

    @ManyToMany(mappedBy = "profesores", fetch = FetchType.LAZY)
    @JsonIgnore
    var cursos: MutableSet<Curso> = mutableSetOf(),

    // Pagos DE alquiler (profesor PAGA al instituto)
    @OneToMany(mappedBy = "profesor", cascade = [CascadeType.ALL])
    val pagosAlquilerRealizados: MutableList<PagoAlquiler> = mutableListOf(),

    // Pagos DE comisión (profesor RECIBE del instituto)
    @OneToMany(mappedBy = "profesor", cascade = [CascadeType.ALL])
    val pagosComisionRecibidos: MutableList<PagoComision> = mutableListOf()
) : Rol(usuario = usuario) {

    override fun puedeGestionarCurso(curso: Curso): Boolean {
        return curso is CursoAlquiler && cursos.contains(curso)
    }

    override fun puedeRegistrarPago(inscripcion: Inscripcion): Boolean {
        return inscripcion.curso is CursoAlquiler &&
                cursos.contains(inscripcion.curso)
    }

    override fun puedeAsignarBeneficio(inscripcion: Inscripcion): Boolean {
        return inscripcion.curso is CursoAlquiler &&
                cursos.contains(inscripcion.curso)
    }

    fun cursosActivos(): List<Curso> {
        return cursos.filter { it.fechaBaja == null }
    }

    fun actualizarEstado(){
        usuario.estado = if (cursosActivos().isNotEmpty()) EstadoType.ACTIVO else EstadoType.INACTIVO
    }

    // Pagos que el profesor REALIZÓ al instituto (alquileres)
    fun obtenerPagosRealizados(): List<PagoAlquiler> {
        return pagosAlquilerRealizados.filter { it.estaActivo() }
    }

    // Pagos que el profesor RECIBIÓ del instituto (comisiones)
    fun obtenerPagosRecibidos(): List<PagoComision> {
        return pagosComisionRecibidos.filter { it.estaActivo() }
    }
}

@Entity
@DiscriminatorValue("ALUMNO")
class RolAlumno(
    usuario: Usuario,

    @OneToMany(mappedBy = "alumno", cascade = [CascadeType.ALL], orphanRemoval = true)
    @JsonIgnore
    var inscripciones: MutableList<Inscripcion> = mutableListOf()

) : Rol(usuario = usuario) {

    override fun puedeGestionarCurso(curso: Curso): Boolean = false

    override fun puedeRegistrarPago(inscripcion: Inscripcion): Boolean = false

    override fun puedeAsignarBeneficio(inscripcion: Inscripcion): Boolean = false

}

@Entity
@DiscriminatorValue("PORTERIA")
class RolPorteria(
    usuario: Usuario,
    fechaAlta: LocalDate = LocalDate.now(),
    fechaBaja: LocalDate? = null
) : Rol(
    usuario = usuario,
    fechaAlta = fechaAlta,
    fechaBaja = fechaBaja
) {
    override fun puedeGestionarCurso(curso: Curso): Boolean = false

    override fun puedeRegistrarPago(inscripcion: Inscripcion): Boolean = false

    override fun puedeAsignarBeneficio(inscripcion: Inscripcion): Boolean = false

}