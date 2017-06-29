package com.sequenceiq.cloudbreak.conf;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.jasypt.encryption.pbe.PBEStringCleanablePasswordEncryptor;
import org.jasypt.encryption.pbe.PBEStringEncryptor;
import org.jasypt.exceptions.EncryptionOperationNotPossibleException;
import org.jasypt.hibernate4.encryptor.HibernatePBEEncryptorRegistry;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EncryptedStringConfig {

    @Inject
    @Qualifier("PBEStringCleanablePasswordEncryptor")
    private PBEStringCleanablePasswordEncryptor encryptor;

    @Inject
    @Qualifier("LegacyPBEStringCleanablePasswordEncryptor")
    private PBEStringCleanablePasswordEncryptor legacyEncryptor;

    @PostConstruct
    public void register() {
        HibernatePBEEncryptorRegistry registry = HibernatePBEEncryptorRegistry.getInstance();
        registry.registerPBEStringEncryptor("hibernateStringEncryptor", new PBEStringEncryptor() {

            @Override
            public String encrypt(String message) {
                return encryptor.encrypt(message);
            }

            @Override
            public String decrypt(String encryptedMessage) {
                try {
                    return encryptor.decrypt(encryptedMessage);
                } catch (EncryptionOperationNotPossibleException e) {
                    try {
                        return legacyEncryptor.decrypt(encryptedMessage);
                    } catch (EncryptionOperationNotPossibleException el) {
                        return encryptedMessage;
                    }
                }
            }

            @Override
            public void setPassword(String password) {
                encryptor.setPassword(password);
            }
        });
    }
}