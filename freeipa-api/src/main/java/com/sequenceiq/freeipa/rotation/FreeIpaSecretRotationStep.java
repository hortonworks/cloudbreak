package com.sequenceiq.freeipa.rotation;

import com.sequenceiq.cloudbreak.rotation.SecretRotationStep;

public enum FreeIpaSecretRotationStep implements SecretRotationStep {

    FREEIPA_ADMIN_USER_PASSWORD,
    FREEIPA_DIRECTORY_MANAGER_PASSWORD,
    SALT_PILLAR_UPDATE,
    CCMV2_JUMPGATE,
    LAUNCH_TEMPLATE,
    SALT_STATE_APPLY,
    SALT_STATE_RUN,
    FREEIPA_USER_PASSWORD;

    @Override
    public Class<? extends Enum<?>> getClazz() {
        return FreeIpaSecretRotationStep.class;
    }

    @Override
    public String value() {
        return name();
    }
}
