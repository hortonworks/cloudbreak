package com.sequenceiq.freeipa.api.rotation;

import static com.sequenceiq.cloudbreak.rotation.secret.step.CommonSecretRotationStep.CUSTOM_JOB;
import static com.sequenceiq.cloudbreak.rotation.secret.step.CommonSecretRotationStep.SERVICE_CONFIG;
import static com.sequenceiq.cloudbreak.rotation.secret.step.CommonSecretRotationStep.USER_DATA;
import static com.sequenceiq.cloudbreak.rotation.secret.step.CommonSecretRotationStep.VAULT;

import java.util.List;

import com.sequenceiq.cloudbreak.rotation.secret.SecretType;
import com.sequenceiq.cloudbreak.rotation.secret.step.SecretRotationStep;

public enum FreeIpaSecretType implements SecretType {

    SALT_BOOT_SECRETS(List.of(VAULT, CUSTOM_JOB, SERVICE_CONFIG, USER_DATA));

    private final List<SecretRotationStep> steps;

    FreeIpaSecretType(List<SecretRotationStep> steps) {
        this.steps = steps;
    }

    @Override
    public List<SecretRotationStep> getSteps() {
        return steps;
    }
}
