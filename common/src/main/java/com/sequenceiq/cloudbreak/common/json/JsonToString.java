package com.sequenceiq.cloudbreak.common.json;

import jakarta.persistence.AttributeConverter;

public class JsonToString implements AttributeConverter<Json, String> {
    @Override
    public String convertToDatabaseColumn(Json attribute) {
        if (attribute != null) {
            return attribute.getValue();
        }
        return null;
    }

    @Override
    public Json convertToEntityAttribute(String dbData) {
        return new Json(dbData);
    }
}
