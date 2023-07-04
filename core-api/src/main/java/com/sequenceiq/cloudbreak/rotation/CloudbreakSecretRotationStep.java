package com.sequenceiq.cloudbreak.rotation;

public enum CloudbreakSecretRotationStep implements SecretRotationStep {
    CM_USER,
    SALT_PILLAR,
    SALT_STATE_APPLY,
    SALT_STATE_RUN,
    CLUSTER_PROXY,
    CM_SERVICE
}
