package com.sequenceiq.cloudbreak.rotation;

public enum CloudbreakSecretRotationStep implements SecretRotationStep {

    CM_USER,
    SALT_PILLAR,
    SALT_STATE_APPLY,
    SALT_STATE_RUN,
    CLUSTER_PROXY_REREGISTER,
    CLUSTER_PROXY_UPDATE,
    CM_SERVICE_ROLE_RESTART,
    CM_SERVICE;

    @Override
    public Class<? extends Enum<?>> getClazz() {
        return CloudbreakSecretRotationStep.class;
    }

    @Override
    public String value() {
        return name();
    }
}
