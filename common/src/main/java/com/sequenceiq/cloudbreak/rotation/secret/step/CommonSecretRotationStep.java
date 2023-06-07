package com.sequenceiq.cloudbreak.rotation.secret.step;

public enum CommonSecretRotationStep implements SecretRotationStep {
    VAULT,
    CUSTOM_JOB,
    REDBEAMS_ROTATE_POLLING,
    CLOUDBREAK_ROTATE_POLLING
}
