package com.sequenceiq.cloudbreak.rotation.secret.type;

import static com.sequenceiq.cloudbreak.rotation.secret.SecretRotationStep.PROVIDER_DATABASE_ROOT_PASSWORD;
import static com.sequenceiq.cloudbreak.rotation.secret.SecretRotationStep.VAULT;

import java.util.List;

import com.sequenceiq.cloudbreak.rotation.secret.SecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.secret.SecretType;

public enum RedbeamsSecretType implements SecretType {

    REDBEAMS_EXTERNAL_DATABASE_ROOT_PASSWORD(List.of(VAULT, PROVIDER_DATABASE_ROOT_PASSWORD));

    private final List<SecretRotationStep> steps;

    RedbeamsSecretType(List<SecretRotationStep> steps) {
        this.steps = steps;
    }

    @Override
    public List<SecretRotationStep> getSteps() {
        return steps;
    }
}
