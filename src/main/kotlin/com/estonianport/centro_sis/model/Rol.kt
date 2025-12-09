package com.estonianport.centro_sis.model

import com.estonianport.centro_sis.model.enums.BeneficioType
import com.estonianport.centro_sis.model.enums.PagoType
import jakarta.persistence.*
import java.time.LocalDate

@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "rol_type", discriminatorType = DiscriminatorType.STRING)
abstract class Rol(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    var usuario: Usuario,

    @Column
    var fechaAlta: LocalDate = LocalDate.now(),

    @Column
    var fechaBaja: LocalDate? = null
)

@Entity
@DiscriminatorValue("ADMINISTRADOR")
class RolAdmin(
    usuario: Usuario
) : Rol(usuario = usuario)

@Entity
@DiscriminatorValue("PROFESOR")
class RolProfesor(
    usuario: Usuario,

    @ManyToOne(fetch = FetchType.LAZY)
    var curso: Curso? = null
) : Rol(usuario = usuario)

@Entity
@DiscriminatorValue("ALUMNO")
class RolAlumno(
    usuario: Usuario,

    @OneToMany(mappedBy = "alumno", cascade = [CascadeType.ALL], orphanRemoval = true)
    var inscripciones: MutableList<Inscripcion> = mutableListOf(),

    @Enumerated(EnumType.STRING)
    @Column(nullable = true)
    var beneficioType: BeneficioType? = null
) : Rol(usuario = usuario) {

    var puntos: Int = 0

    fun calcularArancelFinal(arancelBase: Double, beneficioFactory: BeneficioFactory): Double {
        val beneficio = beneficioType?.let { beneficioFactory.getStrategy(it) }
        return beneficio?.aplicarBeneficio(arancelBase, usuario, null) ?: arancelBase
    }

     //Inscribe al alumno en un curso con un tipo de pago específico
    fun inscribirEnCurso(curso: Curso, tipoPago: PagoType): Inscripcion {
        // Verificar que el curso acepte ese tipo de pago
        if (!curso.tiposPago.any { it.tipoPago == tipoPago }) {
            throw IllegalArgumentException("El curso ${curso.nombre} no acepta el tipo de pago $tipoPago")
        }

        val inscripcion = Inscripcion(
            alumno = this,
            curso = curso,
            tipoPago = tipoPago
        )

        inscripciones.add(inscripcion)
        return inscripcion
    }

    fun getInscripcionPorCurso(curso: Curso): Inscripcion {
        return inscripciones.find { it.curso.id == curso.id && it.fechaBaja == null }
            ?: throw NoSuchElementException("El alumno no está inscrito en el curso ${curso.nombre}")
    }

     //Obtiene todas las inscripciones activas
    fun getInscripcionesActivas(): List<Inscripcion> {
        return inscripciones.filter { it.fechaBaja == null }
    }

     //Verifica si está al día en todos sus cursos
    fun estaAlDiaEnTodos(beneficioFactory: BeneficioFactory): Boolean {
        return getInscripcionesActivas().all { it.estaAlDia() }
    }

     //Calcula la deuda total pendiente
    fun calcularDeudaTotal(beneficioFactory: BeneficioFactory): Double {
        return getInscripcionesActivas().sumOf { it.calcularDeudaPendiente(beneficioFactory) }
    }
}
