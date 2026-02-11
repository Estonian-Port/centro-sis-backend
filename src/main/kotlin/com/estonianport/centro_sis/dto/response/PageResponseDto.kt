package com.estonianport.centro_sis.dto.response

import org.springframework.data.domain.Page

/**
 * DTO genérico para respuestas paginadas
 * Estructura JSON estable y compatible
 */
data class PageResponse<T>(
    val content: List<T>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
    val first: Boolean,
    val last: Boolean,
    val empty: Boolean
) {
    companion object {
        /**
         * Convierte un Page de Spring Data a PageResponse
         */
        fun <T> from(page: Page<T>): PageResponse<T> {
            return PageResponse(
                content = page.content,
                page = page.number,
                size = page.size,
                totalElements = page.totalElements,
                totalPages = page.totalPages,
                first = page.isFirst,
                last = page.isLast,
                empty = page.isEmpty
            )
        }
    }
}