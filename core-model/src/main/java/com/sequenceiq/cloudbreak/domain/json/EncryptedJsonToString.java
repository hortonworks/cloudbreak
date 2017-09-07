package com.sequenceiq.cloudbreak.domain.json;

import java.util.Optional;

import org.jasypt.encryption.pbe.StandardPBEStringEncryptor;
import org.jasypt.exceptions.EncryptionOperationNotPossibleException;

public class EncryptedJsonToString extends JsonToString {

    private static final StandardPBEStringEncryptor ENCRYPTOR = new StandardPBEStringEncryptor();

    private static final StandardPBEStringEncryptor LEGACY_ENCRYPTOR = new StandardPBEStringEncryptor();

    static {
        String secret = Optional.ofNullable(System.getProperty("cb.client.secret")).orElse(System.getenv("CB_CLIENT_SECRET"));
        ENCRYPTOR.setPassword(secret);
        LEGACY_ENCRYPTOR.setPassword("cbsecret2015");
    }

    @Override
    public String convertToDatabaseColumn(Json attribute) {
        String json = super.convertToDatabaseColumn(attribute);
        return ENCRYPTOR.encrypt(json);
    }

    @Override
    public Json convertToEntityAttribute(String dbData) {
        String json;
        try {
            json = ENCRYPTOR.decrypt(dbData);
        } catch (EncryptionOperationNotPossibleException e) {
            try {
                json = LEGACY_ENCRYPTOR.decrypt(dbData);
            } catch (EncryptionOperationNotPossibleException ex) {
                json = dbData;
            }
        }
        return super.convertToEntityAttribute(json);
    }
}
