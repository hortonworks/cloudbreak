package com.sequenceiq.redbeams.rotation;

import com.sequenceiq.cloudbreak.rotation.SecretRotationStep;

public enum RedbeamsSecretRotationStep implements SecretRotationStep {
    PROVIDER_DATABASE_ROOT_PASSWORD;

    @Override
    public Class<? extends Enum<?>> getClazz() {
        return RedbeamsSecretRotationStep.class;
    }

    @Override
    public String value() {
        return name();
    }
}
