package com.sequenceiq.sdx.rotation;

import static com.sequenceiq.cloudbreak.rotation.CommonSecretRotationStep.CLOUDBREAK_ROTATE_POLLING;
import static com.sequenceiq.cloudbreak.rotation.CommonSecretRotationStep.REDBEAMS_ROTATE_POLLING;

import java.util.List;

import com.sequenceiq.cloudbreak.rotation.SecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.SecretType;

public enum DatalakeSecretType implements SecretType {

    DATALAKE_CB_CM_ADMIN_PASSWORD(List.of(CLOUDBREAK_ROTATE_POLLING)),
    DATALAKE_MGMT_CM_ADMIN_PASSWORD(List.of(CLOUDBREAK_ROTATE_POLLING)),
    DATALAKE_DATABASE_ROOT_PASSWORD(List.of(REDBEAMS_ROTATE_POLLING, CLOUDBREAK_ROTATE_POLLING)),
    DATALAKE_CM_DB_PASSWORD(List.of(CLOUDBREAK_ROTATE_POLLING)),

    DATALAKE_USER_KEYPAIR(List.of(CLOUDBREAK_ROTATE_POLLING)),
    DATALAKE_IDBROKER_CERT(List.of(CLOUDBREAK_ROTATE_POLLING)),
    DATALAKE_CM_SERVICE_DB_PASSWORD(List.of(CLOUDBREAK_ROTATE_POLLING)),
    DL_CM_SERVICE_SHARED_DB(List.of(CLOUDBREAK_ROTATE_POLLING), true),
    DH_CM_SERVICE_SHARED_DB(List.of(CLOUDBREAK_ROTATE_POLLING), true),
    DATALAKE_SALT_BOOT_SECRETS(List.of(CLOUDBREAK_ROTATE_POLLING));

    private final List<SecretRotationStep> steps;

    private final boolean multiCluster;

    DatalakeSecretType(List<SecretRotationStep> steps) {
        this.steps = steps;
        this.multiCluster = false;
    }

    DatalakeSecretType(List<SecretRotationStep> steps, boolean multiCluster) {
        this.steps = steps;
        this.multiCluster = multiCluster;
    }

    @Override
    public List<SecretRotationStep> getSteps() {
        return steps;
    }

    @Override
    public boolean multiSecret() {
        return multiCluster;
    }

    @Override
    public Class<? extends Enum<?>> getClazz() {
        return DatalakeSecretType.class;
    }

    @Override
    public String value() {
        return name();
    }
}
