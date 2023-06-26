package com.sequenceiq.freeipa.service.rotation;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.rotation.secret.SecretType;
import com.sequenceiq.cloudbreak.rotation.secret.application.ApplicationSecretRotationInformation;
import com.sequenceiq.freeipa.api.rotation.FreeIpaSecretType;

@Component
public class FreeipaSecretRotationInformation implements ApplicationSecretRotationInformation {

    @Override
    public Class<? extends SecretType> supportedSecretType() {
        return FreeIpaSecretType.class;
    }
}
