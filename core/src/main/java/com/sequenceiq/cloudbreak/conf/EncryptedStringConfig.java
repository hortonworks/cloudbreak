package com.sequenceiq.cloudbreak.conf;

import javax.annotation.PostConstruct;

import org.jasypt.hibernate4.encryptor.HibernatePBEEncryptorRegistry;
import org.jasypt.hibernate4.encryptor.HibernatePBEStringEncryptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EncryptedStringConfig {

    @Value("${cb.client.secret}")
    private String clientSecret;

    @PostConstruct
    public void register() {
        HibernatePBEStringEncryptor encryptor = new HibernatePBEStringEncryptor();
        encryptor.setPassword(clientSecret);
        HibernatePBEEncryptorRegistry registry = HibernatePBEEncryptorRegistry.getInstance();
        registry.registerPBEStringEncryptor("hibernateStringEncryptor", encryptor.getEncryptor());
    }
}
