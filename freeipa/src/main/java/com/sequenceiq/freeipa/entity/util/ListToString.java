package com.sequenceiq.freeipa.entity.util;

import java.io.IOException;
import java.util.List;

import javax.persistence.AttributeConverter;

import com.fasterxml.jackson.core.type.TypeReference;
import com.sequenceiq.cloudbreak.common.json.Json;

public abstract class ListToString<U> implements AttributeConverter<List<U>, String> {

    public abstract <U> TypeReference<List<U>> getTypeReference();

    @Override
    public String convertToDatabaseColumn(List<U> entityData) {
        if (entityData != null) {
            return new Json(entityData).getValue();
        }
        return null;
    }

    @Override
    public List<U> convertToEntityAttribute(String dbData) {
        try {
            return new Json(dbData).get(getTypeReference());
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to parse list from Json.", e);
        }
    }
}
