package com.sequenceiq.cloudbreak.rotation.secret.context;

import com.sequenceiq.cloudbreak.rotation.secret.RotationContext;
import com.sequenceiq.cloudbreak.rotation.secret.SecretType;

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