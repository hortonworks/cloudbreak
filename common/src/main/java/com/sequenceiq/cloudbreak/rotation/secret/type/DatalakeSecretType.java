package com.sequenceiq.cloudbreak.rotation.secret.type;

import java.util.List;

import com.sequenceiq.cloudbreak.rotation.secret.SecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.secret.SecretType;

public enum DatalakeSecretType implements SecretType {

    CLOUDBREAK_CM_ADMIN_PASSWORD(List.of()),
    MGMT_CM_ADMIN_PASSWORD(List.of());

    private final List<SecretRotationStep> steps;

    DatalakeSecretType(List<SecretRotationStep> steps) {
        this.steps = steps;
    }

    @Override
    public List<SecretRotationStep> getSteps() {
        return steps;
    }
}
