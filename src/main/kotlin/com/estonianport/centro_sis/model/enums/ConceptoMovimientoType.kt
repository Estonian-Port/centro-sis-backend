package com.estonianport.centro_sis.model.enums

enum class ConceptoMovimiento {
    PAGO_ALUMNO,           // Alumno paga curso
    PAGO_ALQUILER_PROFESOR, // Profesor paga alquiler al instituto
    PAGO_COMISION_PROFESOR, // Instituto paga comisión a profesor
    GASTO_OPERATIVO,       // Gastos generales
    OTRO_INGRESO,          // Otros ingresos
    OTRO_EGRESO            // Otros egresos
}