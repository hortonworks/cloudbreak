package com.sequenceiq.cloudbreak.rotation;

import static com.sequenceiq.cloudbreak.rotation.CloudbreakSecretRotationStep.CLUSTER_PROXY_REREGISTER;
import static com.sequenceiq.cloudbreak.rotation.CloudbreakSecretRotationStep.CLUSTER_PROXY_UPDATE;
import static com.sequenceiq.cloudbreak.rotation.CloudbreakSecretRotationStep.CM_SERVICE;
import static com.sequenceiq.cloudbreak.rotation.CloudbreakSecretRotationStep.CM_SERVICE_ROLE_RESTART;
import static com.sequenceiq.cloudbreak.rotation.CloudbreakSecretRotationStep.CM_USER;
import static com.sequenceiq.cloudbreak.rotation.CloudbreakSecretRotationStep.SALT_PILLAR;
import static com.sequenceiq.cloudbreak.rotation.CloudbreakSecretRotationStep.SALT_STATE_APPLY;
import static com.sequenceiq.cloudbreak.rotation.CloudbreakSecretRotationStep.SALT_STATE_RUN;
import static com.sequenceiq.cloudbreak.rotation.CloudbreakSecretRotationStep.UMS_DATABUS_CREDENTIAL;
import static com.sequenceiq.cloudbreak.rotation.CommonSecretRotationStep.CUSTOM_JOB;
import static com.sequenceiq.cloudbreak.rotation.CommonSecretRotationStep.FREEIPA_ROTATE_POLLING;
import static com.sequenceiq.cloudbreak.rotation.CommonSecretRotationStep.REDBEAMS_ROTATE_POLLING;
import static com.sequenceiq.cloudbreak.rotation.CommonSecretRotationStep.SALTBOOT_CONFIG;
import static com.sequenceiq.cloudbreak.rotation.CommonSecretRotationStep.USER_DATA;
import static com.sequenceiq.cloudbreak.rotation.CommonSecretRotationStep.VAULT;
import static com.sequenceiq.cloudbreak.rotation.MultiSecretType.DEMO_MULTI_SECRET;
import static com.sequenceiq.cloudbreak.rotation.SecretTypeFlag.INTERNAL;
import static com.sequenceiq.cloudbreak.rotation.SecretTypeFlag.POST_FLOW;
import static com.sequenceiq.cloudbreak.rotation.SecretTypeFlag.SKIP_SALT_UPDATE;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public enum CloudbreakSecretType implements SecretType {

    CM_ADMIN_PASSWORD(List.of(VAULT, CM_USER, CLUSTER_PROXY_REREGISTER), Set.of(SKIP_SALT_UPDATE)),
    CM_MGMT_ADMIN_PASSWORD(List.of(VAULT, CM_USER, CLUSTER_PROXY_REREGISTER), Set.of(SKIP_SALT_UPDATE)),
    EXTERNAL_DATABASE_ROOT_PASSWORD(List.of(REDBEAMS_ROTATE_POLLING, SALT_PILLAR)),
    CM_DB_PASSWORD(List.of(VAULT, SALT_PILLAR, SALT_STATE_APPLY, CUSTOM_JOB)),
    USER_KEYPAIR(List.of(SALT_STATE_RUN, CUSTOM_JOB)),
    GATEWAY_CERT(List.of(VAULT, CUSTOM_JOB, CM_SERVICE_ROLE_RESTART, CLUSTER_PROXY_UPDATE)),
    CM_SERVICES_DB_PASSWORD(List.of(VAULT, SALT_PILLAR, SALT_STATE_APPLY, CM_SERVICE)),
    SALT_BOOT_SECRETS(List.of(VAULT, CUSTOM_JOB, SALTBOOT_CONFIG, USER_DATA)),
    CM_SERVICE_SHARED_DB(List.of(SALT_PILLAR, CM_SERVICE), MultiSecretType.CM_SERVICE_SHARED_DB),
    CM_INTERMEDIATE_CA_CERT(List.of(CUSTOM_JOB), Set.of(POST_FLOW)),
    LDAP_BIND_PASSWORD(List.of(FREEIPA_ROTATE_POLLING, CUSTOM_JOB, SALT_STATE_APPLY)),
    SSSD_IPA_PASSWORD(List.of(FREEIPA_ROTATE_POLLING, SALT_PILLAR), Set.of(SKIP_SALT_UPDATE)),
    DBUS_UMS_ACCESS_KEY(List.of(UMS_DATABUS_CREDENTIAL, CUSTOM_JOB)),
    DEMO_SECRET(List.of(VAULT, CUSTOM_JOB), DEMO_MULTI_SECRET, Set.of(SKIP_SALT_UPDATE, INTERNAL)),
    STACK_ENCRYPTION_KEYS(List.of(CUSTOM_JOB), Set.of(SKIP_SALT_UPDATE)),
    LUKS_VOLUME_PASSPHRASE(List.of(CUSTOM_JOB, SALT_STATE_APPLY)),
    SALT_MASTER_KEY_PAIR(List.of(VAULT, CUSTOM_JOB), Set.of(SKIP_SALT_UPDATE)),
    SALT_SIGN_KEY_PAIR(List.of(VAULT, CUSTOM_JOB), Set.of(SKIP_SALT_UPDATE)),
    // internal DL related secrets
    INTERNAL_DATALAKE_EXTERNAL_DATABASE_ROOT_PASSWORD(List.of(SALT_PILLAR), Set.of(INTERNAL)),
    INTERNAL_DATALAKE_CM_SERVICE_SHARED_DB(List.of(VAULT, SALT_PILLAR, SALT_STATE_APPLY, CM_SERVICE), Set.of(INTERNAL)),
    INTERNAL_DATALAKE_DEMO_SECRET(List.of(CUSTOM_JOB), Set.of(SKIP_SALT_UPDATE, INTERNAL)),
    INTERNAL_DATALAKE_CM_INTERMEDIATE_CA_CERT(List.of(CUSTOM_JOB), Set.of(INTERNAL)),
    INTERNAL_DATALAKE_IDBROKER_CERT(List.of(VAULT, SALT_PILLAR, SALT_STATE_APPLY, CM_SERVICE_ROLE_RESTART, CUSTOM_JOB), Set.of(INTERNAL)),
    INTERNAL_DATALAKE_SSSD_IPA_PASSWORD(List.of(SALT_PILLAR), Set.of(SKIP_SALT_UPDATE, INTERNAL));

    private final List<SecretRotationStep> steps;

    private final Optional<MultiSecretType> multiSecretType;

    private final Set<SecretTypeFlag> flags;

    CloudbreakSecretType(List<SecretRotationStep> steps) {
        this.steps = steps;
        this.multiSecretType = Optional.empty();
        this.flags = Set.of();
    }

    CloudbreakSecretType(List<SecretRotationStep> steps, MultiSecretType multiSecretType) {
        this.steps = steps;
        this.multiSecretType = Optional.ofNullable(multiSecretType);
        this.flags = Set.of();
    }

    CloudbreakSecretType(List<SecretRotationStep> steps, MultiSecretType multiSecretType, Set<SecretTypeFlag> flags) {
        this.steps = steps;
        this.multiSecretType = Optional.ofNullable(multiSecretType);
        this.flags = flags;
    }

    CloudbreakSecretType(List<SecretRotationStep> steps, Set<SecretTypeFlag> flags) {
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
        return CloudbreakSecretType.class;
    }

    @Override
    public String value() {
        return name();
    }
}
