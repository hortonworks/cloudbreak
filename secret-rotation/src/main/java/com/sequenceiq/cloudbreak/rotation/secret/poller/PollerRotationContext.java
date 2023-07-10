package com.sequenceiq.cloudbreak.rotation.secret.poller;

import com.sequenceiq.cloudbreak.rotation.SecretType;
import com.sequenceiq.cloudbreak.rotation.common.RotationContext;

public class PollerRotationContext extends RotationContext {

    private final SecretType secretType;

    public PollerRotationContext(String resourceCrn, SecretType secretType) {
        super(resourceCrn);
        this.secretType = secretType;
    }

    public SecretType getSecretType() {
        return secretType;
    }
}