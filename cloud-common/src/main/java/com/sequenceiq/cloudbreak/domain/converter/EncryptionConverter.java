package com.sequenceiq.cloudbreak.domain.converter;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import org.jasypt.encryption.StringEncryptor;
import org.jasypt.exceptions.EncryptionOperationNotPossibleException;
import org.springframework.stereotype.Component;

@Component
@Converter
public class EncryptionConverter implements AttributeConverter<String, String> {

    private static StringEncryptor encryptor;

    private static StringEncryptor legacyEncryptor;

    @Inject
    @Named("PBEStringCleanablePasswordEncryptor")
    private StringEncryptor pbeStringCleanablePasswordEncryptor;

    @Inject
    @Named("LegacyPBEStringCleanablePasswordEncryptor")
    private StringEncryptor legacyPbeStringCleanablePasswordEncryptor;

    @PostConstruct
    public void init() {
        encryptor = pbeStringCleanablePasswordEncryptor;
        legacyEncryptor = legacyPbeStringCleanablePasswordEncryptor;
    }

    @Override
    public String convertToDatabaseColumn(String attribute) {
        return encryptor.encrypt(attribute);
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        try {
            return encryptor.decrypt(dbData);
        } catch (EncryptionOperationNotPossibleException e) {
            try {
                return legacyEncryptor.decrypt(dbData);
            } catch (EncryptionOperationNotPossibleException ignored) {
                return dbData;
            }
        }
    }
}

