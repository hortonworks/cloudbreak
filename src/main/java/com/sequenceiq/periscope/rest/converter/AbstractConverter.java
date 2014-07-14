package com.sequenceiq.periscope.rest.converter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.sequenceiq.periscope.rest.json.Json;

public abstract class AbstractConverter<J extends Json, E> implements Converter<J, E> {

    @Override
    public E convert(J source) {
        throw new UnsupportedOperationException();
    }

    @Override
    public J convert(E source) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Collection<E> convertAllFromJson(Collection<J> jsonList) {
        List<E> entityList = new ArrayList<>(jsonList.size());
        for (J json : jsonList) {
            entityList.add(convert(json));
        }
        return entityList;
    }

    @Override
    public Collection<J> convertAllToJson(Collection<E> entityList) {
        List<J> jsonList = new ArrayList<>(entityList.size());
        for (E entity : entityList) {
            jsonList.add(convert(entity));
        }
        return jsonList;
    }
}
