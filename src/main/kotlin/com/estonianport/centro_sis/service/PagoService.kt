package com.estonianport.centro_sis.service

import com.estonianport.centro_sis.common.GenericServiceImpl
import com.estonianport.centro_sis.dto.response.PagoResponseDto
import com.estonianport.centro_sis.mapper.PagoMapper
import com.estonianport.centro_sis.model.Curso
import com.estonianport.centro_sis.model.CursoAlquiler
import com.estonianport.centro_sis.model.CursoComision
import com.estonianport.centro_sis.model.Inscripcion
import com.estonianport.centro_sis.model.MovimientoFinanciero
import com.estonianport.centro_sis.model.Pago
import com.estonianport.centro_sis.model.PagoAlquiler
import com.estonianport.centro_sis.model.RolProfesor
import com.estonianport.centro_sis.model.Usuario
import com.estonianport.centro_sis.model.enums.ConceptoMovimiento
import com.estonianport.centro_sis.model.enums.TipoMovimiento
import com.estonianport.centro_sis.repository.MovimientoFinancieroRepository
import com.estonianport.centro_sis.repository.PagoRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class PagoService : GenericServiceImpl<Pago, Long>() {

    @Autowired
    lateinit var pagoRepository: PagoRepository

    @Autowired
    lateinit var movimientoRepository: MovimientoFinancieroRepository

    override val dao: PagoRepository
        get() = pagoRepository

    override fun delete(id: Long) {
        val pago: Pago = pagoRepository.findById(id).get()
        pago.fechaBaja = LocalDate.now()
        pagoRepository.save(pago)
    }


        fun calcularIngresosMensuales(): Double {
            val desde = LocalDate.now().withDayOfMonth(1)
            val hasta = desde.plusMonths(1).minusDays(1)

            return pagoRepository.findByFechaBetweenAndFechaBajaIsNull(desde, hasta)
                .sumOf { it.monto }.toDouble()
        }


    fun registrarPagoAlumno(inscripcion: Inscripcion, usuario: Usuario): PagoResponseDto {
        // 1. Registrar el pago en la inscripción (como ya lo tienes)
        val pago = inscripcion.registrarPago()
        pagoRepository.save(pago)

        // 2. Registrar el movimiento financiero
        val movimiento = MovimientoFinanciero(
            tipo = TipoMovimiento.INGRESO,
            concepto = ConceptoMovimiento.PAGO_ALUMNO,
            monto = pago.monto,
            usuarioInvolucrado = pago.getAlumno().usuario,
            curso = pago.getCurso(),
            pagoId = pago.id,
            registradoPor = usuario // Usuario que registró el pago
        )
        movimientoRepository.save(movimiento)

        return PagoMapper.buildPagoResponseDto(pago)
    }

    fun registrarPagoAlquilerProfesor(curso: CursoAlquiler, usuarioActual: Usuario) : PagoResponseDto {
        // 1. Registrar pago de alquiler
        val pagoAlquiler = curso.registrarPagoAlquiler()
        pagoRepository.save(pagoAlquiler)

        // 2. Registrar movimiento
        val movimiento = MovimientoFinanciero(
            tipo = TipoMovimiento.INGRESO,
            concepto = ConceptoMovimiento.PAGO_ALQUILER_PROFESOR,
            monto = pagoAlquiler.monto,
            usuarioInvolucrado = pagoAlquiler.profesor.usuario,
            curso = curso,
            pagoAlquilerId = pagoAlquiler.id,
            registradoPor = usuarioActual
        )
        movimientoRepository.save(movimiento)

        return PagoMapper.buildPagoResponseDto(pagoAlquiler)
    }

    fun registrarComisionProfesor(curso: CursoComision, profesor: RolProfesor, usuarioActual : Usuario): PagoResponseDto {
        // 1. Registrar pago de comisión
        val pagoComision = curso.registrarPagoComision(profesor)
        pagoRepository.save(pagoComision)

        // 2. Registrar movimiento
        val movimiento = MovimientoFinanciero(
            tipo = TipoMovimiento.EGRESO,
            concepto = ConceptoMovimiento.PAGO_COMISION_PROFESOR,
            monto = curso.calcularPagoProfesor(),
            usuarioInvolucrado = profesor.usuario,
            curso = curso,
            registradoPor = usuarioActual
        )
        movimientoRepository.save(movimiento)

        return PagoMapper.buildPagoResponseDto(pagoComision)
    }
}