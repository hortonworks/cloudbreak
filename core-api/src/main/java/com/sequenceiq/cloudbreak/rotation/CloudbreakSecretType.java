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
import static com.sequenceiq.cloudbreak.rotation.SecretTypeFlag.INTERNAL;
import static com.sequenceiq.cloudbreak.rotation.SecretTypeFlag.POST_FLOW;
import static com.sequenceiq.cloudbreak.rotation.SecretTypeFlag.SKIP_SALT_UPDATE;

import java.util.List;
import java.util.Set;

public enum CloudbreakSecretType implements SecretType {

    CM_ADMIN_PASSWORD(List.of(VAULT, CM_USER, CLUSTER_PROXY_REREGISTER), Set.of(SKIP_SALT_UPDATE)),
    EXTERNAL_DATABASE_ROOT_PASSWORD(List.of(REDBEAMS_ROTATE_POLLING, SALT_PILLAR)),
    CM_DB_PASSWORD(List.of(VAULT, SALT_PILLAR, SALT_STATE_APPLY, CUSTOM_JOB)),
    USER_KEYPAIR(List.of(SALT_STATE_RUN, CUSTOM_JOB)),
    GATEWAY_CERT(List.of(VAULT, CUSTOM_JOB, CM_SERVICE_ROLE_RESTART, CLUSTER_PROXY_UPDATE)),
    CM_SERVICES_DB_PASSWORD(List.of(VAULT, SALT_PILLAR, SALT_STATE_APPLY, CM_SERVICE)),
    SALT_BOOT_SECRETS(List.of(VAULT, CUSTOM_JOB, SALTBOOT_CONFIG, USER_DATA)),
    CM_INTERMEDIATE_CA_CERT(List.of(CUSTOM_JOB), Set.of(POST_FLOW)),
    LDAP_BIND_PASSWORD(List.of(FREEIPA_ROTATE_POLLING, CUSTOM_JOB, SALT_STATE_APPLY)),
    SSSD_IPA_PASSWORD(List.of(FREEIPA_ROTATE_POLLING, SALT_PILLAR), Set.of(SKIP_SALT_UPDATE)),
    DBUS_UMS_ACCESS_KEY(List.of(UMS_DATABUS_CREDENTIAL, CUSTOM_JOB, CM_SERVICE)),
    STACK_ENCRYPTION_KEYS(List.of(CUSTOM_JOB), Set.of(SKIP_SALT_UPDATE)),
    LUKS_VOLUME_PASSPHRASE(List.of(CUSTOM_JOB, SALT_STATE_APPLY)),
    SALT_MASTER_KEY_PAIR(List.of(VAULT, CUSTOM_JOB), Set.of(SKIP_SALT_UPDATE)),
    SALT_SIGN_KEY_PAIR(List.of(VAULT, CUSTOM_JOB), Set.of(SKIP_SALT_UPDATE)),
    SALT_PASSWORD(List.of(VAULT, CUSTOM_JOB), Set.of(SKIP_SALT_UPDATE)),
    NGINX_CLUSTER_SSL_CERT_PRIVATE_KEY(List.of(SALT_STATE_APPLY, CUSTOM_JOB)),
    COMPUTE_MONITORING_CREDENTIALS(List.of(VAULT, CUSTOM_JOB)),
    EMBEDDED_DB_SSL_CERT(List.of(CUSTOM_JOB, SALT_STATE_APPLY)),
    // internal DL related secrets
    INTERNAL_DATALAKE_EXTERNAL_DATABASE_ROOT_PASSWORD(List.of(SALT_PILLAR), Set.of(INTERNAL)),
    INTERNAL_DATALAKE_CM_SERVICE_SHARED_DB(List.of(VAULT, SALT_PILLAR, SALT_STATE_APPLY, CM_SERVICE, CUSTOM_JOB), Set.of(INTERNAL)),
    INTERNAL_DATALAKE_DEMO_SECRET(List.of(CUSTOM_JOB), Set.of(SKIP_SALT_UPDATE, INTERNAL)),
    INTERNAL_DATALAKE_CM_INTERMEDIATE_CA_CERT(List.of(CUSTOM_JOB), Set.of(INTERNAL)),
    INTERNAL_DATALAKE_IDBROKER_CERT(List.of(VAULT, SALT_PILLAR, SALT_STATE_APPLY, CM_SERVICE_ROLE_RESTART, CUSTOM_JOB), Set.of(INTERNAL)),
    INTERNAL_DATALAKE_SSSD_IPA_PASSWORD(List.of(SALT_PILLAR), Set.of(SKIP_SALT_UPDATE, INTERNAL)),
    // internal secrets
    DEMO_SECRET(List.of(VAULT, CUSTOM_JOB), Set.of(SKIP_SALT_UPDATE, INTERNAL));

    private final List<SecretRotationStep> steps;

    private final Set<SecretTypeFlag> flags;

    CloudbreakSecretType(List<SecretRotationStep> steps) {
        this.steps = steps;
        this.flags = Set.of();
    }

    CloudbreakSecretType(List<SecretRotationStep> steps, Set<SecretTypeFlag> flags) {
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
        return CloudbreakSecretType.class;
    }

    @Override
    public String value() {
        return name();
    }
}
