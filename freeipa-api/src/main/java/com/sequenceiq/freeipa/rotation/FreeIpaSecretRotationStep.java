package com.sequenceiq.freeipa.rotation;

import com.sequenceiq.cloudbreak.rotation.SecretRotationStep;

public enum FreeIpaSecretRotationStep implements SecretRotationStep {

    FREEIPA_ADMIN_USER_PASSWORD,
    FREEIPA_DIRECTORY_MANAGER_PASSWORD,
    FREEIPA_PILLAR_UPDATE;

    @Override
    public Class<? extends Enum<?>> getClazz() {
        return FreeIpaSecretRotationStep.class;
    }

    @Override
    public String value() {
        return name();
    }
}
