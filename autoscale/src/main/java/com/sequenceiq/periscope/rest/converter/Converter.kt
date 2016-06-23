package com.sequenceiq.periscope.rest.converter

import com.sequenceiq.periscope.api.model.Json

interface Converter<J : Json, E> {

    /**
     * Converts a class marked with @Json to
     * and entity.

     * @param source source json
     * *
     * @return converted class
     */
    fun convert(source: J): E

    /**
     * Converts a class back to json

     * @param source class to be converted from
     * *
     * @return json
     */
    fun convert(source: E): J

    /**
     * Bulk conversion from json.

     * @param jsonList collection of json classes
     * *
     * @return collection of converted classes
     */
    fun convertAllFromJson(jsonList: List<J>): List<E>

    /**
     * Bulk conversion to json.

     * @param entityList collection of entity classes
     * *
     * @return collection of converted classes
     */
    fun convertAllToJson(entityList: List<E>): List<J>

}
