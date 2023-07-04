package com.sequenceiq.datalake.rotation;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.rotation.SecretType;
import com.sequenceiq.cloudbreak.rotation.config.ApplicationSecretRotationInformation;
import com.sequenceiq.sdx.rotation.DatalakeSecretType;

@Component
public class DatalakeSecretRotationInformation implements ApplicationSecretRotationInformation {

    @Override
    public Class<? extends SecretType> supportedSecretType() {
        return DatalakeSecretType.class;
    }
}
