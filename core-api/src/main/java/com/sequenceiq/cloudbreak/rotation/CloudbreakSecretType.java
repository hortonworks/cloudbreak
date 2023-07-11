package com.sequenceiq.cloudbreak.rotation;

import static com.sequenceiq.cloudbreak.rotation.CloudbreakSecretRotationStep.CLUSTER_PROXY;
import static com.sequenceiq.cloudbreak.rotation.CloudbreakSecretRotationStep.CM_SERVICE;
import static com.sequenceiq.cloudbreak.rotation.CloudbreakSecretRotationStep.CM_USER;
import static com.sequenceiq.cloudbreak.rotation.CloudbreakSecretRotationStep.SALT_PILLAR;
import static com.sequenceiq.cloudbreak.rotation.CloudbreakSecretRotationStep.SALT_STATE_APPLY;
import static com.sequenceiq.cloudbreak.rotation.CloudbreakSecretRotationStep.SALT_STATE_RUN;
import static com.sequenceiq.cloudbreak.rotation.CommonSecretRotationStep.CUSTOM_JOB;
import static com.sequenceiq.cloudbreak.rotation.CommonSecretRotationStep.REDBEAMS_ROTATE_POLLING;
import static com.sequenceiq.cloudbreak.rotation.CommonSecretRotationStep.SALTBOOT_CONFIG;
import static com.sequenceiq.cloudbreak.rotation.CommonSecretRotationStep.USER_DATA;
import static com.sequenceiq.cloudbreak.rotation.CommonSecretRotationStep.VAULT;

import java.util.List;

public enum CloudbreakSecretType implements SecretType {
    CLUSTER_CB_CM_ADMIN_PASSWORD(List.of(VAULT, CM_USER, CLUSTER_PROXY)),
    CLUSTER_MGMT_CM_ADMIN_PASSWORD(List.of(VAULT, CM_USER, CLUSTER_PROXY)),
    DATAHUB_EXTERNAL_DATABASE_ROOT_PASSWORD(List.of(REDBEAMS_ROTATE_POLLING, SALT_PILLAR)),
    DATALAKE_EXTERNAL_DATABASE_ROOT_PASSWORD(List.of(SALT_PILLAR)),
    CLUSTER_CM_DB_PASSWORD(List.of(VAULT, SALT_PILLAR, SALT_STATE_APPLY, CUSTOM_JOB)),
    USER_KEYPAIR(List.of(SALT_STATE_RUN, CUSTOM_JOB)),
    CLUSTER_CM_SERVICES_DB_PASSWORD(List.of(VAULT, SALT_PILLAR, SALT_STATE_APPLY, CM_SERVICE)),
    SALT_BOOT_SECRETS(List.of(VAULT, CUSTOM_JOB, SALTBOOT_CONFIG, USER_DATA));

    private final List<SecretRotationStep> steps;

    CloudbreakSecretType(List<SecretRotationStep> steps) {
        this.steps = steps;
    }

    @Override
    public List<SecretRotationStep> getSteps() {
        return steps;
    }

    @Override
    public Class<? extends Enum<?>> getClazz() {
        return CloudbreakSecretType.class;
    }

    @Override
    public String value() {
        return name();
    }
}
