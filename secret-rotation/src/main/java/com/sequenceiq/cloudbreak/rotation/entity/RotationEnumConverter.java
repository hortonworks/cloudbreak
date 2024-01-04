package com.sequenceiq.cloudbreak.rotation.entity;

import java.io.IOException;

import jakarta.persistence.AttributeConverter;

import com.sequenceiq.cloudbreak.rotation.SerializableRotationEnum;
import com.sequenceiq.cloudbreak.rotation.serialization.SecretRotationEnumSerializationUtil;

public class RotationEnumConverter implements AttributeConverter<SerializableRotationEnum, String> {

    @Override
    public String convertToDatabaseColumn(SerializableRotationEnum attribute) {
        return SecretRotationEnumSerializationUtil.serialize(attribute);
    }

    @Override
    public SerializableRotationEnum convertToEntityAttribute(String dbData) {
        try {
            return SecretRotationEnumSerializationUtil.deserialize(dbData);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
