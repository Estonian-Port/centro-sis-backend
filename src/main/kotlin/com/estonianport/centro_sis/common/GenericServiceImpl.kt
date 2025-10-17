package com.estonianport.centro_sis.common

import com.estonianport.centro_sis.repository.CursoRepository
import org.springframework.stereotype.Service
import java.io.Serializable

@Service
abstract class GenericServiceImpl<T : Any, ID : Serializable> : GenericService<T, ID> {

    abstract val dao : CursoRepository

    override fun save(entity: T): T {
        return dao.save(entity)
    }

    override fun delete(id: ID) {
        dao.deleteById(id)
    }

    override fun get(id: ID): T? {
        val obj = dao.findById(id)
        return if (obj.isPresent) {
            obj.get()
        } else null
    }

    override fun getAll(): MutableList<T>? {
        val returnList: MutableList<T> = ArrayList()
        dao.findAll().forEach { obj : T -> returnList.add(obj) }
        return returnList
    }

    fun count(): Long {
        return dao.count()
    }

}