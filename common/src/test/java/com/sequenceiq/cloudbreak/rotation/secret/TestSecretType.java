package com.sequenceiq.cloudbreak.rotation.secret;

import static com.sequenceiq.cloudbreak.rotation.secret.TestSecretRotationStep.STEP;

import java.util.List;

import com.sequenceiq.cloudbreak.rotation.secret.step.SecretRotationStep;

public enum TestSecretType implements SecretType {
    TEST,
    TEST_2;

    @Override
    public List<SecretRotationStep> getSteps() {
        return List.of(STEP);
    }
}
