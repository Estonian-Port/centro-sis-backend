package com.estonianport.centro_sis.service

import com.estonianport.centro_sis.common.GenericServiceImpl
import com.estonianport.centro_sis.model.Pago
import com.estonianport.centro_sis.repository.PagoRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class PagoService : GenericServiceImpl<Pago, Long>() {

    @Autowired
    lateinit var pagoRepository: PagoRepository

    override val dao: PagoRepository
        get() = pagoRepository

    override fun delete(id: Long) {
        val pago : Pago = pagoRepository.findById(id).get()
        pago.fechaBaja = LocalDate.now()
        pagoRepository.save(pago)
    }
}