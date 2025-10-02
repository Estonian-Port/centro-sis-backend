package com.estonianport.centro_sis.common

import java.io.Serializable

interface GenericService<T : Any, ID : Serializable> {
    fun save(entity: T): T

    fun delete(id: ID)

    fun get(id: ID): T?

    fun getAll() : MutableList<T>?
}