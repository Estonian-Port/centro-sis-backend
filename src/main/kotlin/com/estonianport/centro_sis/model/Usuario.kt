package com.estonianport.centro_sis.model

import com.estonianport.centro_sis.dto.UsuarioAbmDTO
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
    val esAdministrador: Boolean = false,

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "usuario_roles", joinColumns = [JoinColumn(name = "usuario_id")])
    @Column(name = "rol")
    val roles: Set<RolType> = setOf(RolType.ALUMNO),

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

    @ManyToMany
    @JoinTable(
        name = "usuario_cursos_activos",
        joinColumns = [JoinColumn(name = "usuario_id")],
        inverseJoinColumns = [JoinColumn(name = "curso_id")]
    )
    val cursosActivos: MutableSet<Curso> = mutableSetOf()

    @ManyToMany
    @JoinTable(
        name = "usuario_cursos_baja",
        joinColumns = [JoinColumn(name = "usuario_id")],
        inverseJoinColumns = [JoinColumn(name = "curso_id")]
    )
    val cursosDadosDeBaja: MutableSet<Curso> = mutableSetOf()

    @ElementCollection(targetClass = BeneficioType::class, fetch = FetchType.EAGER)
    @CollectionTable(
        name = "usuario_beneficios",
        joinColumns = [JoinColumn(name = "usuario_id")]
    )
    @Enumerated(EnumType.STRING)
    val beneficios: MutableSet<BeneficioType> = mutableSetOf()

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
            estado = EstadoType.ACTIVO
        }
    }
}