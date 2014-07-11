package com.sequenceiq.periscope.rest.converter;

import java.util.Collection;

import com.sequenceiq.periscope.rest.json.Json;

public interface Converter<J extends Json, E> {

    /**
     * Converts a class marked with @Json to
     * and entity.
     *
     * @param source source json
     * @return converted class
     */
    E convert(J source);

    /**
     * Converts a class back to json
     *
     * @param source class to be converted from
     * @return json
     */
    J convert(E source);

    /**
     * Bulk conversion from json.
     *
     * @param jsonList collection of json classes
     * @return collection of converted classes
     */
    Collection<E> convertAllFromJson(Collection<J> jsonList);

    /**
     * Bulk conversion to json.
     *
     * @param entityList collection of entity classes
     * @return collection of converted classes
     */
    Collection<J> convertAllToJson(Collection<E> entityList);

}
