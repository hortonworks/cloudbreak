package com.sequenceiq.cloudbreak.conf;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.jasypt.encryption.StringEncryptor;
import org.jasypt.encryption.pbe.PBEStringEncryptor;
import org.jasypt.exceptions.EncryptionOperationNotPossibleException;
import org.jasypt.hibernate4.encryptor.HibernatePBEEncryptorRegistry;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EncryptedStringConfig {

    @Inject
    private ApplicationContext applicationContext;

    @PostConstruct
    public void register() {
        HibernatePBEEncryptorRegistry registry = HibernatePBEEncryptorRegistry.getInstance();
        registry.registerPBEStringEncryptor("hibernateStringEncryptor", new PBEStringEncryptor() {

            @Override
            public String encrypt(String message) {
                StringEncryptor encryptor = (StringEncryptor) applicationContext.getBean("PBEStringCleanablePasswordEncryptor");
                return encryptor.encrypt(message);
            }

            @Override
            public String decrypt(String encryptedMessage) {
                StringEncryptor encryptor = (StringEncryptor) applicationContext.getBean("PBEStringCleanablePasswordEncryptor");
                try {
                    return encryptor.decrypt(encryptedMessage);
                } catch (EncryptionOperationNotPossibleException e) {
                    StringEncryptor legacyEncryptor = (StringEncryptor) applicationContext.getBean("LegacyPBEStringCleanablePasswordEncryptor");
                    try {
                        return legacyEncryptor.decrypt(encryptedMessage);
                    } catch (EncryptionOperationNotPossibleException ignored) {
                        return encryptedMessage;
                    }
                }
            }

            @Override
            public void setPassword(String password) {
            }
        });
    }
}