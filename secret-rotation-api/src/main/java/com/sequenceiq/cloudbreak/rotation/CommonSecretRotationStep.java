package com.sequenceiq.cloudbreak.rotation;

public enum CommonSecretRotationStep implements SecretRotationStep {
    VAULT,
    CUSTOM_JOB,
    REDBEAMS_ROTATE_POLLING,
    CLOUDBREAK_ROTATE_POLLING,
    FREEIPA_ROTATE_POLLING,
    SALTBOOT_CONFIG,
    USER_DATA;

    @Override
    public Class<? extends Enum<?>> getClazz() {
        return CommonSecretRotationStep.class;
    }

    @Override
    public String value() {
        return name();
    }

}
