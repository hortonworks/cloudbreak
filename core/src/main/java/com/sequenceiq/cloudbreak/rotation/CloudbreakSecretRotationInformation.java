package com.sequenceiq.cloudbreak.rotation;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.rotation.secret.SecretType;
import com.sequenceiq.cloudbreak.rotation.secret.application.ApplicationSecretRotationInformation;

@Component
public class CloudbreakSecretRotationInformation implements ApplicationSecretRotationInformation {

    @Override
    public Class<? extends SecretType> supportedSecretType() {
        return CloudbreakSecretType.class;
    }
}
