package com.sequenceiq.cloudbreak.rotation.secret;

public enum SecretRotationStep {
    VAULT,
    CM_USER,
    SALT_PILLAR,
    SALT_STATE_APPLY,
    CLUSTER_PROXY,
    PROVIDER_DATABASE_ROOT_PASSWORD,
    REDBEAMS_ROTATE_POLLING,
    CLOUDBREAK_ROTATE_POLLING
}
