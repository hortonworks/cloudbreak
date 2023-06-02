package com.sequenceiq.cloudbreak.rotation.secret.type;

import static com.sequenceiq.cloudbreak.rotation.secret.SecretRotationStep.CLOUDBREAK_ROTATE_POLLING;
import static com.sequenceiq.cloudbreak.rotation.secret.SecretRotationStep.REDBEAMS_ROTATE_POLLING;

import java.util.List;

import com.sequenceiq.cloudbreak.rotation.secret.SecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.secret.SecretType;

public enum DatalakeSecretType implements SecretType {

    CLOUDBREAK_CM_ADMIN_PASSWORD(List.of()),
    MGMT_CM_ADMIN_PASSWORD(List.of()),
    DATALAKE_DATABASE_ROOT_PASSWORD(List.of(REDBEAMS_ROTATE_POLLING, CLOUDBREAK_ROTATE_POLLING));

    private final List<SecretRotationStep> steps;

    DatalakeSecretType(List<SecretRotationStep> steps) {
        this.steps = steps;
    }

    @Override
    public List<SecretRotationStep> getSteps() {
        return steps;
    }
}
