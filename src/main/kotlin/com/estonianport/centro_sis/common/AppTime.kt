package com.estonianport.centro_sis.common

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Zona horaria de la aplicación.
 *
 * El servidor de producción corre en una imagen Alpine cuya zona por defecto es UTC
 * (el `ENV TZ` del Dockerfile no tiene efecto sin el paquete `tzdata`). Por eso toda
 * la lógica sensible a fechas/horas debe fijar explícitamente esta zona en lugar de
 * usar `LocalDate.now()` / `LocalDateTime.now()` (que toman la zona por defecto de la JVM).
 *
 * `ZoneId.of(...)` resuelve siempre correctamente porque la JDK trae su propia base de
 * datos de zonas horarias, independientemente del SO.
 */
object AppTime {
    val ZONA: ZoneId = ZoneId.of("America/Argentina/Buenos_Aires")

    fun hoy(): LocalDate = LocalDate.now(ZONA)

    fun ahora(): LocalDateTime = LocalDateTime.now(ZONA)
}
