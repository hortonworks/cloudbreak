package com.sequenceiq.freeipa.api.rotation;

import static com.sequenceiq.cloudbreak.rotation.CommonSecretRotationStep.CUSTOM_JOB;
import static com.sequenceiq.cloudbreak.rotation.CommonSecretRotationStep.SALTBOOT_CONFIG;
import static com.sequenceiq.cloudbreak.rotation.CommonSecretRotationStep.USER_DATA;
import static com.sequenceiq.cloudbreak.rotation.CommonSecretRotationStep.VAULT;
import static com.sequenceiq.freeipa.rotation.FreeIpaSecretRotationStep.FREEIPA_ADMIN_USER_PASSWORD;
import static com.sequenceiq.freeipa.rotation.FreeIpaSecretRotationStep.FREEIPA_DIRECTORY_MANAGER_PASSWORD;
import static com.sequenceiq.freeipa.rotation.FreeIpaSecretRotationStep.FREEIPA_PILLAR_UPDATE;

import java.util.List;

import com.sequenceiq.cloudbreak.rotation.SecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.SecretType;

public enum FreeIpaSecretType implements SecretType {

    FREEIPA_ADMIN_PASSWORD(List.of(VAULT, FREEIPA_ADMIN_USER_PASSWORD, FREEIPA_DIRECTORY_MANAGER_PASSWORD, FREEIPA_PILLAR_UPDATE)),
    FREEIPA_SALT_BOOT_SECRETS(List.of(VAULT, CUSTOM_JOB, SALTBOOT_CONFIG, USER_DATA));

    private final List<SecretRotationStep> steps;

    FreeIpaSecretType(List<SecretRotationStep> steps) {
        this.steps = steps;
    }

    @Override
    public List<SecretRotationStep> getSteps() {
        return steps;
    }

    @Override
    public Class<? extends Enum<?>> getClazz() {
        return FreeIpaSecretType.class;
    }

    @Override
    public String value() {
        return name();
    }
}
