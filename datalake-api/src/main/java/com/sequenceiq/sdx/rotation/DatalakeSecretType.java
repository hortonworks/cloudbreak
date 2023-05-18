package com.sequenceiq.sdx.rotation;

import static com.sequenceiq.cloudbreak.rotation.secret.step.CommonSecretRotationStep.CLOUDBREAK_ROTATE_POLLING;
import static com.sequenceiq.cloudbreak.rotation.secret.step.CommonSecretRotationStep.REDBEAMS_ROTATE_POLLING;

import java.util.List;

import com.sequenceiq.cloudbreak.rotation.secret.SecretType;
import com.sequenceiq.cloudbreak.rotation.secret.step.SecretRotationStep;

public enum DatalakeSecretType implements SecretType {

    DATALAKE_CB_CM_ADMIN_PASSWORD(List.of(CLOUDBREAK_ROTATE_POLLING)),
    DATALAKE_MGMT_CM_ADMIN_PASSWORD(List.of(CLOUDBREAK_ROTATE_POLLING)),
    DATALAKE_DATABASE_ROOT_PASSWORD(List.of(REDBEAMS_ROTATE_POLLING, CLOUDBREAK_ROTATE_POLLING)),
    DATALAKE_CM_DB_PASSWORD(List.of(CLOUDBREAK_ROTATE_POLLING)),

    DATALAKE_USER_KEYPAIR(List.of(CLOUDBREAK_ROTATE_POLLING)),
    DATALAKE_CM_SERVICE_DB_PASSWORD(List.of(CLOUDBREAK_ROTATE_POLLING));

    private final List<SecretRotationStep> steps;

    DatalakeSecretType(List<SecretRotationStep> steps) {
        this.steps = steps;
    }

    @Override
    public List<SecretRotationStep> getSteps() {
        return steps;
    }
}
