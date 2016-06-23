package com.sequenceiq.periscope.rest.converter

import java.util.ArrayList

import com.sequenceiq.periscope.api.model.Json

abstract class AbstractConverter<J : Json, E> : Converter<J, E> {

    override fun convert(source: J): E {
        throw UnsupportedOperationException()
    }

    override fun convert(source: E): J {
        throw UnsupportedOperationException()
    }

    override fun convertAllFromJson(jsonList: List<J>): List<E> {
        val entityList = ArrayList<E>(jsonList.size)
        for (json in jsonList) {
            entityList.add(convert(json))
        }
        return entityList
    }

    override fun convertAllToJson(entityList: List<E>): List<J> {
        val jsonList = ArrayList<J>(entityList.size)
        for (entity in entityList) {
            jsonList.add(convert(entity))
        }
        return jsonList
    }
}
