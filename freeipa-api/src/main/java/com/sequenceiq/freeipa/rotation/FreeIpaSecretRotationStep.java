package com.sequenceiq.freeipa.rotation;

import com.sequenceiq.cloudbreak.rotation.secret.step.SecretRotationStep;

public enum FreeIpaSecretRotationStep implements SecretRotationStep {

    FREEIPA_ADMIN_USER_PASSWORD,
    FREEIPA_DIRECTORY_MANAGER_PASSWORD,
    FREEIPA_PILLAR_UPDATE;

}
