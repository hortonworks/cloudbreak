package com.sequenceiq.cloudbreak.rotation;

import java.util.Set;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.rotation.secret.SecretType;
import com.sequenceiq.cloudbreak.rotation.secret.application.ApplicationSecretRotationInformation;
import com.sequenceiq.cloudbreak.rotation.secret.type.CloudbreakSecretType;

@Component
public class CloudbreakSecretRotationInformation implements ApplicationSecretRotationInformation {

    @Override
    public Set<Class<? extends SecretType>> supportedSecretTypes() {
        return Set.of(CloudbreakSecretType.class);
    }
}
