package com.sequenceiq.redbeams.rotation;

import com.sequenceiq.cloudbreak.rotation.secret.step.SecretRotationStep;

public enum RedbeamsSecretRotationStep implements SecretRotationStep {
    PROVIDER_DATABASE_ROOT_PASSWORD;
}
