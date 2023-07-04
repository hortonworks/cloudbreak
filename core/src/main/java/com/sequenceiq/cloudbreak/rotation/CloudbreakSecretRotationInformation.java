package com.sequenceiq.cloudbreak.rotation;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.rotation.config.ApplicationSecretRotationInformation;

@Component
public class CloudbreakSecretRotationInformation implements ApplicationSecretRotationInformation {

    @Override
    public Class<? extends SecretType> supportedSecretType() {
        return CloudbreakSecretType.class;
    }
}
