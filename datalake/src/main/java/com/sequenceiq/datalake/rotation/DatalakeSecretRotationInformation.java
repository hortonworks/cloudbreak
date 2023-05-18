package com.sequenceiq.datalake.rotation;

import java.util.Set;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.rotation.secret.SecretType;
import com.sequenceiq.cloudbreak.rotation.secret.application.ApplicationSecretRotationInformation;
import com.sequenceiq.cloudbreak.rotation.secret.type.DatalakeSecretType;

@Component
public class DatalakeSecretRotationInformation implements ApplicationSecretRotationInformation {

    @Override
    public Set<Class<? extends SecretType>> supportedSecretTypes() {
        return Set.of(DatalakeSecretType.class);
    }
}
