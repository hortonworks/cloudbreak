package com.sequenceiq.cloudbreak.service.secret.domain;

import javax.persistence.AttributeConverter;

public class SecretToString implements AttributeConverter<Secret, String> {

    @Override
    public String convertToDatabaseColumn(Secret attribute) {
        return attribute != null ? attribute.getSecret() : null;
    }

    @Override
    public Secret convertToEntityAttribute(String dbData) {
        return new SecretProxy(dbData);
    }
}
