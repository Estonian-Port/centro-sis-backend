package com.estonianport.centro_sis.model

import com.estonianport.centro_sis.model.enums.BeneficioType
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
@DiscriminatorValue("ADMIN")
class RolAdmin(
    usuario: Usuario
) : Rol(usuario = usuario)

@Entity
@DiscriminatorValue("PROFESOR")
class RolProfesor(
    usuario: Usuario,

    @ManyToOne(fetch = FetchType.LAZY)
    var curso: Curso
) : Rol(usuario = usuario)

@Entity
@DiscriminatorValue("ALUMNO")
class RolAlumno(
    usuario: Usuario,

    @ManyToOne(fetch = FetchType.LAZY)
    var curso: Curso,

    @OneToMany(mappedBy = "alumno", cascade = [CascadeType.ALL], orphanRemoval = true)
    var pagos: MutableList<Pago> = mutableListOf(),

    @Enumerated(EnumType.STRING)
    @Column(nullable = true)
    var beneficioType: BeneficioType? = null
) : Rol(usuario = usuario) {
    
    fun calcularArancelFinal(arancelBase: Double, beneficioFactory: BeneficioFactory): Double {
        val beneficio = beneficioType?.let { beneficioFactory.getStrategy(it) }
        return beneficio?.aplicarBeneficio(arancelBase, usuario, curso) ?: arancelBase
    }
}
