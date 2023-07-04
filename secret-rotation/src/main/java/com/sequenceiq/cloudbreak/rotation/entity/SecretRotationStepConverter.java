package com.sequenceiq.cloudbreak.rotation.entity;

import javax.persistence.AttributeConverter;

import com.sequenceiq.cloudbreak.rotation.SecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.serialization.SecretRotationEnumSerializationUtil;

public class SecretRotationStepConverter implements AttributeConverter<SecretRotationStep, String> {
    @Override
    public String convertToDatabaseColumn(SecretRotationStep attribute) {
        return SecretRotationEnumSerializationUtil.enumToMapString((Enum) attribute);
    }

    @Override
    public SecretRotationStep convertToEntityAttribute(String dbData) {
        try {
            return (SecretRotationStep) SecretRotationEnumSerializationUtil.getEnum(SecretRotationEnumSerializationUtil.mapStringToMap(dbData));
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
}
