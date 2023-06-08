package com.sequenceiq.cloudbreak.orchestrator.rotation;

import com.sequenceiq.cloudbreak.rotation.secret.step.SecretRotationStep;

public enum OrchestratorSecretRotationStep implements SecretRotationStep {
    SALT_STATE_APPLY
}
