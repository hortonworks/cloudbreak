package com.sequenceiq.freeipa.service.rotation;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.rotation.SecretType;
import com.sequenceiq.cloudbreak.rotation.config.ApplicationSecretRotationInformation;
import com.sequenceiq.freeipa.api.rotation.FreeIpaSecretType;

@Component
public class FreeIpaApplicationSecretRotationInformation implements ApplicationSecretRotationInformation {

    @Override
    public Class<? extends SecretType> supportedSecretType() {
        return FreeIpaSecretType.class;
    }
}
