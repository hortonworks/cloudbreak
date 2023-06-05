package com.sequenceiq.redbeams.rotation;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.rotation.secret.SecretType;
import com.sequenceiq.cloudbreak.rotation.secret.application.ApplicationSecretRotationInformation;
import com.sequenceiq.cloudbreak.rotation.secret.type.RedbeamsSecretType;

@Component
public class RedbeamsSecretRotationInformation implements ApplicationSecretRotationInformation {

    @Override
    public Class<? extends SecretType> supportedSecretType() {
        return RedbeamsSecretType.class;
    }
}
