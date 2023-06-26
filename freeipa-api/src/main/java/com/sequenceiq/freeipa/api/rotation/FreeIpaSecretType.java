package com.sequenceiq.freeipa.api.rotation;

import static com.sequenceiq.cloudbreak.rotation.secret.step.CommonSecretRotationStep.CUSTOM_JOB;
import static com.sequenceiq.cloudbreak.rotation.secret.step.CommonSecretRotationStep.SERVICE_CONFIG;
import static com.sequenceiq.cloudbreak.rotation.secret.step.CommonSecretRotationStep.USER_DATA;
import static com.sequenceiq.cloudbreak.rotation.secret.step.CommonSecretRotationStep.VAULT;
import static com.sequenceiq.freeipa.rotation.FreeIpaSecretRotationStep.FREEIPA_ADMIN_USER_PASSWORD;
import static com.sequenceiq.freeipa.rotation.FreeIpaSecretRotationStep.FREEIPA_DIRECTORY_MANAGER_PASSWORD;
import static com.sequenceiq.freeipa.rotation.FreeIpaSecretRotationStep.FREEIPA_PILLAR_UPDATE;

import java.util.List;

import com.sequenceiq.cloudbreak.rotation.secret.SecretType;
import com.sequenceiq.cloudbreak.rotation.secret.step.SecretRotationStep;

public enum FreeIpaSecretType implements SecretType {

    SALT_BOOT_SECRETS(List.of(VAULT, CUSTOM_JOB, SERVICE_CONFIG, USER_DATA)),
    FREEIPA_ADMIN_PASSWORD(List.of(VAULT, FREEIPA_ADMIN_USER_PASSWORD, FREEIPA_DIRECTORY_MANAGER_PASSWORD, FREEIPA_PILLAR_UPDATE));

    private final List<SecretRotationStep> steps;

    FreeIpaSecretType(List<SecretRotationStep> steps) {
        this.steps = steps;
    }

    @Override
    public List<SecretRotationStep> getSteps() {
        return steps;
    }
}
