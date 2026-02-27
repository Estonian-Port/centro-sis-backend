package com.estonianport.centro_sis.model

import com.estonianport.centro_sis.model.enums.TipoAcceso
import jakarta.persistence.*
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
@Table(
    name = "acceso",
    indexes = [
        Index(name = "idx_acceso_usuario", columnList = "usuario_id"),
        Index(name = "idx_acceso_fecha", columnList = "fecha_hora")
    ]
)
class Acceso(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = true)
    val usuario: Usuario? = null,

    @Column(name = "fecha_hora", nullable = false)
    val fechaHora: LocalDateTime = LocalDateTime.now(),

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_acceso")
    val tipoAcceso: TipoAcceso,

    @Column(nullable = true)
    val invitadoDni: String? = null,

    @Column(nullable = true)
    val invitadoNombre: String? = null
) {
    fun esDeFecha(fecha: LocalDate): Boolean {
        return fechaHora.toLocalDate() == fecha
    }
}