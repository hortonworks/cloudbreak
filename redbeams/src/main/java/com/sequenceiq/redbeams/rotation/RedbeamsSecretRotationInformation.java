package com.sequenceiq.redbeams.rotation;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.rotation.SecretType;
import com.sequenceiq.cloudbreak.rotation.config.ApplicationSecretRotationInformation;

@Component
public class RedbeamsSecretRotationInformation implements ApplicationSecretRotationInformation {

    @Override
    public Class<? extends SecretType> supportedSecretType() {
        return RedbeamsSecretType.class;
    }
}
