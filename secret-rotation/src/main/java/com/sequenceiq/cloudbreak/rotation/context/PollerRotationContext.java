package com.sequenceiq.cloudbreak.rotation.context;

import com.sequenceiq.cloudbreak.rotation.SecretType;
import com.sequenceiq.cloudbreak.rotation.common.RotationContext;

public class PollerRotationContext extends RotationContext {

    private final SecretType secretType;

    public PollerRotationContext(String datalakeCrn, SecretType secretType) {
        super(datalakeCrn);
        this.secretType = secretType;
    }

    public SecretType getSecretType() {
        return secretType;
    }
}