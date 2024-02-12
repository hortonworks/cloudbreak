package com.sequenceiq.cloudbreak.domain;

import com.sequenceiq.cloudbreak.service.secret.domain.Secret;
import com.sequenceiq.cloudbreak.service.secret.domain.SecretToString;

public class BlueprintSecretToString extends SecretToString {

    public static final String BLANK = "BLANK";

    @Override
    public String convertToDatabaseColumn(Secret attribute) {
        if (attribute.equals(Secret.EMPTY)) {
            return BLANK;
        }
        return super.convertToDatabaseColumn(attribute);
    }

    @Override
    public Secret convertToEntityAttribute(String dbData) {
        if (BLANK.equals(dbData)) {
            return Secret.EMPTY;
        }
        return super.convertToEntityAttribute(dbData);
    }
}
