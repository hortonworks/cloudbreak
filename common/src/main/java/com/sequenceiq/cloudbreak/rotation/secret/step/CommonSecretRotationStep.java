package com.sequenceiq.cloudbreak.rotation.secret.step;

public enum CommonSecretRotationStep implements SecretRotationStep {
    VAULT,
    REDBEAMS_ROTATE_POLLING,
    CLOUDBREAK_ROTATE_POLLING
}
