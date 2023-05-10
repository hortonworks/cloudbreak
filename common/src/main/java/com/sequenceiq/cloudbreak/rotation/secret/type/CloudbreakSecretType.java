package com.sequenceiq.cloudbreak.rotation.secret.type;

import static com.sequenceiq.cloudbreak.rotation.secret.SecretRotationStep.CM_USER;
import static com.sequenceiq.cloudbreak.rotation.secret.SecretRotationStep.VAULT;

import java.util.List;

import com.sequenceiq.cloudbreak.rotation.secret.SecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.secret.SecretType;

public enum CloudbreakSecretType implements SecretType {

    CLOUDBREAK_CM_ADMIN_PASSWORD(List.of(VAULT, CM_USER)),
    MGMT_CM_ADMIN_PASSWORD(List.of(VAULT, CM_USER));

    private final List<SecretRotationStep> steps;

    CloudbreakSecretType(List<SecretRotationStep> steps) {
        this.steps = steps;
    }

    @Override
    public List<SecretRotationStep> getSteps() {
        return steps;
    }
}
