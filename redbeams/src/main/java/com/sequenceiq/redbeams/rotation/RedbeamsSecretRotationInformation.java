package com.sequenceiq.redbeams.rotation;

import java.util.Set;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.rotation.secret.SecretType;
import com.sequenceiq.cloudbreak.rotation.secret.application.ApplicationSecretRotationInformation;
import com.sequenceiq.cloudbreak.rotation.secret.type.RedbeamsSecretType;

@Component
public class RedbeamsSecretRotationInformation implements ApplicationSecretRotationInformation {

    @Override
    public Set<Class<? extends SecretType>> supportedSecretTypes() {
        return Set.of(RedbeamsSecretType.class);
    }
}
