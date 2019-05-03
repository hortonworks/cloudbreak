package com.sequenceiq.freeipa.entity.json;

import java.util.Optional;

import org.jasypt.encryption.pbe.StandardPBEStringEncryptor;
import org.jasypt.exceptions.EncryptionOperationNotPossibleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EncryptedJsonToString extends JsonToString {

    private static final Logger LOGGER = LoggerFactory.getLogger(EncryptedJsonToString.class);

    private static final String SECRET;

    static {
        SECRET = Optional.ofNullable(System.getProperty("cb.client.secret")).orElse(System.getenv("CB_CLIENT_SECRET"));
    }

    private final StandardPBEStringEncryptor encryptor = new StandardPBEStringEncryptor();

    private final StandardPBEStringEncryptor legacyEncryptor = new StandardPBEStringEncryptor();

    public EncryptedJsonToString() {
        encryptor.setPassword(SECRET);
        legacyEncryptor.setPassword("cbsecret2015");
    }

    @Override
    public String convertToDatabaseColumn(Json attribute) {
        String json = super.convertToDatabaseColumn(attribute);
        return encryptor.encrypt(json);
    }

    @Override
    public Json convertToEntityAttribute(String dbData) {
        String json;
        try {
            json = encryptor.decrypt(dbData);
        } catch (EncryptionOperationNotPossibleException e) {
            try {
                json = legacyEncryptor.decrypt(dbData);
            } catch (EncryptionOperationNotPossibleException ex) {
                json = dbData;
                LOGGER.info("Cannot decrypt the data. {}", dbData, ex);
            }
        }
        return super.convertToEntityAttribute(json);
    }
}
