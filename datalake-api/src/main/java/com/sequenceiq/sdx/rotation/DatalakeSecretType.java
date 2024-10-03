package com.sequenceiq.sdx.rotation;

import static com.sequenceiq.cloudbreak.rotation.CommonSecretRotationStep.CLOUDBREAK_ROTATE_POLLING;
import static com.sequenceiq.cloudbreak.rotation.CommonSecretRotationStep.FREEIPA_ROTATE_POLLING;
import static com.sequenceiq.cloudbreak.rotation.CommonSecretRotationStep.REDBEAMS_ROTATE_POLLING;
import static com.sequenceiq.cloudbreak.rotation.MultiSecretType.CM_SERVICE_SHARED_DB;
import static com.sequenceiq.cloudbreak.rotation.MultiSecretType.DEMO_MULTI_SECRET;
import static com.sequenceiq.cloudbreak.rotation.SecretTypeFlag.INTERNAL;
import static com.sequenceiq.cloudbreak.rotation.SecretTypeFlag.POST_FLOW;
import static com.sequenceiq.cloudbreak.rotation.SecretTypeFlag.SKIP_SALT_UPDATE;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.sequenceiq.cloudbreak.rotation.MultiSecretType;
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
    DATALAKE_GATEWAY_CERT(List.of(CLOUDBREAK_ROTATE_POLLING)),
    DATALAKE_CM_SERVICE_DB_PASSWORD(List.of(CLOUDBREAK_ROTATE_POLLING)),
    DATALAKE_CM_SERVICE_SHARED_DB(List.of(CLOUDBREAK_ROTATE_POLLING), CM_SERVICE_SHARED_DB),
    DATALAKE_DEMO_SECRET(List.of(CLOUDBREAK_ROTATE_POLLING), DEMO_MULTI_SECRET, Set.of(SKIP_SALT_UPDATE, INTERNAL)),
    DATALAKE_SALT_BOOT_SECRETS(List.of(CLOUDBREAK_ROTATE_POLLING)),
    DATALAKE_SALT_MASTER_KEY_PAIR(List.of(CLOUDBREAK_ROTATE_POLLING), Set.of(SKIP_SALT_UPDATE)),
    DATALAKE_CM_INTERMEDIATE_CA_CERT(List.of(CLOUDBREAK_ROTATE_POLLING), Set.of(POST_FLOW)),
    DATALAKE_LDAP_BIND_PASSWORD(List.of(CLOUDBREAK_ROTATE_POLLING)),
    DATALAKE_SSSD_IPA_PASSWORD(List.of(FREEIPA_ROTATE_POLLING, CLOUDBREAK_ROTATE_POLLING), Set.of(SKIP_SALT_UPDATE)),
    STACK_ENCRYPTION_KEYS(List.of(CLOUDBREAK_ROTATE_POLLING), Set.of(SKIP_SALT_UPDATE)),
    LUKS_VOLUME_PASSPHRASE(List.of(CLOUDBREAK_ROTATE_POLLING));

    private final List<SecretRotationStep> steps;

    private final Optional<MultiSecretType> multiSecretType;

    private final Set<SecretTypeFlag> flags;

    DatalakeSecretType(List<SecretRotationStep> steps) {
        this.steps = steps;
        this.multiSecretType = Optional.empty();
        this.flags = Set.of();
    }

    DatalakeSecretType(List<SecretRotationStep> steps, MultiSecretType multiSecretType) {
        this.steps = steps;
        this.multiSecretType = Optional.ofNullable(multiSecretType);
        this.flags = Set.of();
    }

    DatalakeSecretType(List<SecretRotationStep> steps, MultiSecretType multiSecretType, Set<SecretTypeFlag> flags) {
        this.steps = steps;
        this.multiSecretType = Optional.ofNullable(multiSecretType);
        this.flags = flags;
    }

    DatalakeSecretType(List<SecretRotationStep> steps, Set<SecretTypeFlag> flags) {
        this.steps = steps;
        this.multiSecretType = Optional.empty();
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
    public Optional<MultiSecretType> getMultiSecretType() {
        return multiSecretType;
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
