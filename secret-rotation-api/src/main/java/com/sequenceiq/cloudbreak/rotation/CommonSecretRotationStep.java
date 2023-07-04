package com.sequenceiq.cloudbreak.rotation;

public enum CommonSecretRotationStep implements SecretRotationStep {
    VAULT,
    CUSTOM_JOB,
    REDBEAMS_ROTATE_POLLING,
    CLOUDBREAK_ROTATE_POLLING,
    SERVICE_CONFIG,
    USER_DATA
}
