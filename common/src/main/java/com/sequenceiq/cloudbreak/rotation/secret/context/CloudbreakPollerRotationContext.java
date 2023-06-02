package com.sequenceiq.cloudbreak.rotation.secret.context;

import com.sequenceiq.cloudbreak.rotation.secret.RotationContext;
import com.sequenceiq.cloudbreak.rotation.secret.type.CloudbreakSecretType;

public class CloudbreakPollerRotationContext extends RotationContext {

    private final CloudbreakSecretType secretType;

    public CloudbreakPollerRotationContext(String datalakeCrn, CloudbreakSecretType secretType) {
        super(datalakeCrn);
        this.secretType = secretType;
    }

    public CloudbreakSecretType getSecretType() {
        return secretType;
    }
}