package com.sequenceiq.cloudbreak.rotation.entity;

import javax.persistence.AttributeConverter;

import com.sequenceiq.cloudbreak.rotation.SecretType;
import com.sequenceiq.cloudbreak.rotation.serialization.SecretRotationEnumSerializationUtil;

public class SecretTypeConverter implements AttributeConverter<SecretType, String> {
    @Override
    public String convertToDatabaseColumn(SecretType attribute) {
        return SecretRotationEnumSerializationUtil.enumToMapString((Enum) attribute);
    }

    @Override
    public SecretType convertToEntityAttribute(String dbData) {
        try {
            return (SecretType) SecretRotationEnumSerializationUtil.getEnum(SecretRotationEnumSerializationUtil.mapStringToMap(dbData));
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
}
