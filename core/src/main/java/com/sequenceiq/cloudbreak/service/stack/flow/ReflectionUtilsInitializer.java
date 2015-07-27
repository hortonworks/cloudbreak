package com.sequenceiq.cloudbreak.service.stack.flow;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.jasypt.encryption.pbe.PBEStringCleanablePasswordEncryptor;
import org.springframework.stereotype.Component;

@Component
public class ReflectionUtilsInitializer {

    @Inject
    private PBEStringCleanablePasswordEncryptor encryptor;

    @PostConstruct
    public void init() {
        ReflectionUtils.setEncryptor(encryptor);
    }

}
