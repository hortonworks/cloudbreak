package com.sequenceiq.cloudbreak.conf;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.jasypt.encryption.pbe.PBEStringCleanablePasswordEncryptor;
import org.jasypt.hibernate4.encryptor.HibernatePBEEncryptorRegistry;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EncryptedStringConfig {

    @Inject
    private PBEStringCleanablePasswordEncryptor encryptor;

    @PostConstruct
    public void register() {
        HibernatePBEEncryptorRegistry registry = HibernatePBEEncryptorRegistry.getInstance();
        registry.registerPBEStringEncryptor("hibernateStringEncryptor", encryptor);
    }
}
