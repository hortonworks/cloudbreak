package com.sequenceiq.cloudbreak.rotation.common;

import static com.sequenceiq.cloudbreak.rotation.common.TestSecretRotationStep.STEP;

import java.util.List;

import com.sequenceiq.cloudbreak.rotation.SecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.SecretType;

public enum TestSecretType implements SecretType {
    TEST,
    TEST_2;

    @Override
    public List<SecretRotationStep> getSteps() {
        return List.of(STEP);
    }
}
