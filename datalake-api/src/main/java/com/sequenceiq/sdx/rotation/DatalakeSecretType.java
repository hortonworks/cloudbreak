package com.sequenceiq.sdx.rotation;

import static com.sequenceiq.cloudbreak.rotation.CommonSecretRotationStep.CLOUDBREAK_ROTATE_POLLING;
import static com.sequenceiq.cloudbreak.rotation.CommonSecretRotationStep.REDBEAMS_ROTATE_POLLING;
import static com.sequenceiq.cloudbreak.rotation.SecretTypeFlag.MULTI_SECRET;
import static com.sequenceiq.cloudbreak.rotation.SecretTypeFlag.SKIP_SALT_UPDATE;

import java.util.List;
import java.util.Set;

import com.sequenceiq.cloudbreak.rotation.SecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.SecretType;
import com.sequenceiq.cloudbreak.rotation.SecretTypeFlag;

public enum DatalakeSecretType implements SecretType {

    DATALAKE_CB_CM_ADMIN_PASSWORD(List.of(CLOUDBREAK_ROTATE_POLLING), Set.of(SKIP_SALT_UPDATE)),
    DATALAKE_MGMT_CM_ADMIN_PASSWORD(List.of(CLOUDBREAK_ROTATE_POLLING), Set.of(SKIP_SALT_UPDATE)),
    DATALAKE_DATABASE_ROOT_PASSWORD(List.of(REDBEAMS_ROTATE_POLLING, CLOUDBREAK_ROTATE_POLLING)),
    DATALAKE_CM_DB_PASSWORD(List.of(CLOUDBREAK_ROTATE_POLLING)),
    DATALAKE_USER_KEYPAIR(List.of(CLOUDBREAK_ROTATE_POLLING)),
    DATALAKE_IDBROKER_CERT(List.of(CLOUDBREAK_ROTATE_POLLING)),
    DATALAKE_CM_SERVICE_DB_PASSWORD(List.of(CLOUDBREAK_ROTATE_POLLING)),
    DATALAKE_CM_SERVICE_SHARED_DB(List.of(CLOUDBREAK_ROTATE_POLLING), Set.of(MULTI_SECRET)),
    DATALAKE_DEMO_SECRET(List.of(CLOUDBREAK_ROTATE_POLLING), Set.of(MULTI_SECRET, SKIP_SALT_UPDATE)),
    DATALAKE_SALT_BOOT_SECRETS(List.of(CLOUDBREAK_ROTATE_POLLING));

    private final List<SecretRotationStep> steps;

    private final Set<SecretTypeFlag> flags;

    DatalakeSecretType(List<SecretRotationStep> steps) {
        this.steps = steps;
        this.flags = Set.of();
    }

    DatalakeSecretType(List<SecretRotationStep> steps, Set<SecretTypeFlag> flags) {
        this.steps = steps;
        this.flags = flags;
    }

    @Override
    public List<SecretRotationStep> getSteps() {
        return steps;
    }

    @Override
    public Set<SecretTypeFlag> getFlags() {
        return flags;
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
