package com.sequenceiq.flow.rotation;

import java.util.List;

import com.sequenceiq.cloudbreak.rotation.secret.SecretType;
import com.sequenceiq.cloudbreak.rotation.secret.step.SecretRotationStep;

public enum TestSecretType implements SecretType {
    TEST,
    TEST2;

    @Override
    public List<SecretRotationStep> getSteps() {
        return List.of(TestSecretRotationStep.TEST_STEP);
    }
}
