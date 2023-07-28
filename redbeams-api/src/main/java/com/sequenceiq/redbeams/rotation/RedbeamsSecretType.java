package com.sequenceiq.redbeams.rotation;

import static com.sequenceiq.cloudbreak.rotation.CommonSecretRotationStep.VAULT;
import static com.sequenceiq.redbeams.rotation.RedbeamsSecretRotationStep.PROVIDER_DATABASE_ROOT_PASSWORD;

import java.util.List;

import com.sequenceiq.cloudbreak.rotation.SecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.SecretType;

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

    @Override
    public boolean saltUpdateNeeded() {
        return false;
    }

    @Override
    public Class<? extends Enum<?>> getClazz() {
        return RedbeamsSecretType.class;
    }

    @Override
    public String value() {
        return name();
    }
}
