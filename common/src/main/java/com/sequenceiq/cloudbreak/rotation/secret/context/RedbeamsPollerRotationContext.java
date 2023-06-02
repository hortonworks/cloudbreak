package com.sequenceiq.cloudbreak.rotation.secret.context;

import com.sequenceiq.cloudbreak.rotation.secret.RotationContext;
import com.sequenceiq.cloudbreak.rotation.secret.type.RedbeamsSecretType;

public class RedbeamsPollerRotationContext extends RotationContext {

    private final RedbeamsSecretType secretType;

    public RedbeamsPollerRotationContext(String resourceCrn, RedbeamsSecretType secretType) {
        super(resourceCrn);
        this.secretType = secretType;
    }

    public RedbeamsSecretType getSecretType() {
        return secretType;
    }
}