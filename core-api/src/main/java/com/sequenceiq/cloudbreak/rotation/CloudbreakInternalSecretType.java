package com.sequenceiq.cloudbreak.rotation;

import static com.sequenceiq.cloudbreak.rotation.CloudbreakSecretRotationStep.SALT_PILLAR;

import java.util.List;

public enum CloudbreakInternalSecretType implements SecretType {

    DATALAKE_EXTERNAL_DATABASE_ROOT_PASSWORD(List.of(SALT_PILLAR));

    private final List<SecretRotationStep> steps;

    CloudbreakInternalSecretType(List<SecretRotationStep> steps) {
        this.steps = steps;
    }

    @Override
    public List<SecretRotationStep> getSteps() {
        return steps;
    }

    @Override
    public boolean internal() {
        return true;
    }

    @Override
    public Class<? extends Enum<?>> getClazz() {
        return CloudbreakInternalSecretType.class;
    }

    @Override
    public String value() {
        return name();
    }
}
