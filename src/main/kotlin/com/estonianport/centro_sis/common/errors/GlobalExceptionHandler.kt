package com.estonianport.centro_sis.common.errors

import com.estonianport.centro_sis.dto.response.CustomResponse
import org.springframework.core.annotation.AnnotatedElementUtils
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.context.request.WebRequest
import org.springframework.web.server.ResponseStatusException
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler
import java.util.*

@ControllerAdvice
class GlobalExceptionHandler : ResponseEntityExceptionHandler() {

    @ExceptionHandler(value = [NotFoundException::class, BusinessException::class])
    fun handleCustomExceptions(exception: Exception, webRequest: WebRequest?): ResponseEntity<Any>? {
        val errorCode = resolveAnnotatedResponseStatus(exception)
        return handleExceptionInternal(exception, exception.message, HttpHeaders(), errorCode, webRequest!!)
    }

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgument(ex: IllegalArgumentException): ResponseEntity<CustomResponse> {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
            CustomResponse(
                message = ex.message ?: "Datos inválidos",
                data = null
            )
        )
    }

    @ExceptionHandler(ResponseStatusException::class)
    fun handleResponseStatus(ex: ResponseStatusException): ResponseEntity<CustomResponse> {
        return ResponseEntity.status(ex.statusCode).body(
            CustomResponse(
                message = ex.reason ?: "Error en la solicitud",
                data = null
            )
        )
    }

    private fun resolveAnnotatedResponseStatus(exception: Exception): HttpStatus {
        val annotation = AnnotatedElementUtils.findMergedAnnotation(exception.javaClass, ResponseStatus::class.java)
        return if (Objects.nonNull(annotation)) annotation!!.value else HttpStatus.INTERNAL_SERVER_ERROR
    }
}