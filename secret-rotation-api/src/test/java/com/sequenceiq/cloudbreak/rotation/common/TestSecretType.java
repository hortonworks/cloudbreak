package com.sequenceiq.cloudbreak.rotation.common;

import static com.sequenceiq.cloudbreak.rotation.common.TestSecretRotationStep.STEP;

import java.util.List;

import com.sequenceiq.cloudbreak.rotation.SecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.SecretType;

public enum TestSecretType implements SecretType {
    TEST,
    TEST_2,
    TEST_3,
    TEST_4;

    @Override
    public List<SecretRotationStep> getSteps() {
        return List.of(STEP);
    }

    @Override
    public boolean internal() {
        return value().equals(TEST_3.value());
    }

    @Override
    public boolean multiSecret() {
        return List.of(TEST_2.value(), TEST_4.value()).contains(value());
    }

    @Override
    public Class<? extends Enum<?>> getClazz() {
        return TestSecretType.class;
    }

    @Override
    public String value() {
        return name();
    }
}
