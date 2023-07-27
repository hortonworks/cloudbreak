package com.sequenceiq.cloudbreak.rotation;

public enum CommonSecretRotationStep implements SecretRotationStep {
    VAULT,
    CUSTOM_JOB(true),
    REDBEAMS_ROTATE_POLLING,
    CLOUDBREAK_ROTATE_POLLING,
    SALTBOOT_CONFIG,
    USER_DATA;

    private final boolean skipNotification;

    CommonSecretRotationStep() {
        this(false);
    }

    CommonSecretRotationStep(boolean skipNotification) {
        this.skipNotification = skipNotification;
    }

    @Override
    public Class<? extends Enum<?>> getClazz() {
        return CommonSecretRotationStep.class;
    }

    @Override
    public String value() {
        return name();
    }

    @Override
    public boolean skipNotification() {
        return skipNotification;
    }
}
