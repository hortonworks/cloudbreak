package com.sequenceiq.cloudbreak.rotation.common;

import com.sequenceiq.cloudbreak.rotation.SecretRotationStep;

public enum TestSecretRotationStep implements SecretRotationStep {
    STEP;

    @Override
    public Class<? extends Enum<?>> getClazz() {
        return TestSecretRotationStep.class;
    }

    @Override
    public String value() {
        return name();
    }
}
