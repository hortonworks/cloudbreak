package com.sequenceiq.freeipa.rotation;

import static com.sequenceiq.cloudbreak.rotation.CommonSecretRotationStep.CUSTOM_JOB;
import static com.sequenceiq.cloudbreak.rotation.CommonSecretRotationStep.SALTBOOT_CONFIG;
import static com.sequenceiq.cloudbreak.rotation.CommonSecretRotationStep.USER_DATA;
import static com.sequenceiq.cloudbreak.rotation.CommonSecretRotationStep.VAULT;
import static com.sequenceiq.cloudbreak.rotation.SecretTypeFlag.INTERNAL;
import static com.sequenceiq.cloudbreak.rotation.SecretTypeFlag.SKIP_SALT_UPDATE;
import static com.sequenceiq.freeipa.rotation.FreeIpaSecretRotationStep.CCMV2_JUMPGATE;
import static com.sequenceiq.freeipa.rotation.FreeIpaSecretRotationStep.FREEIPA_ADMIN_USER_PASSWORD;
import static com.sequenceiq.freeipa.rotation.FreeIpaSecretRotationStep.FREEIPA_DIRECTORY_MANAGER_PASSWORD;
import static com.sequenceiq.freeipa.rotation.FreeIpaSecretRotationStep.FREEIPA_UMS_DATABUS_CREDENTIAL;
import static com.sequenceiq.freeipa.rotation.FreeIpaSecretRotationStep.FREEIPA_USER_PASSWORD;
import static com.sequenceiq.freeipa.rotation.FreeIpaSecretRotationStep.LAUNCH_TEMPLATE;
import static com.sequenceiq.freeipa.rotation.FreeIpaSecretRotationStep.SALT_PILLAR_UPDATE;
import static com.sequenceiq.freeipa.rotation.FreeIpaSecretRotationStep.SALT_STATE_APPLY;
import static com.sequenceiq.freeipa.rotation.FreeIpaSecretRotationStep.SALT_STATE_RUN;

import java.util.List;
import java.util.Set;

import com.sequenceiq.cloudbreak.rotation.SecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.SecretType;
import com.sequenceiq.cloudbreak.rotation.SecretTypeFlag;

public enum FreeIpaSecretType implements SecretType {

    // freeipa related secrets
    SALT_BOOT_SECRETS(List.of(VAULT, CUSTOM_JOB, SALTBOOT_CONFIG, USER_DATA, LAUNCH_TEMPLATE)),
    SALT_SIGN_KEY_PAIR(List.of(VAULT, CUSTOM_JOB), Set.of(SKIP_SALT_UPDATE)),
    SALT_MASTER_KEY_PAIR(List.of(VAULT, CUSTOM_JOB), Set.of(SKIP_SALT_UPDATE)),
    CCMV2_JUMPGATE_AGENT_ACCESS_KEY(List.of(CCMV2_JUMPGATE, LAUNCH_TEMPLATE, SALT_PILLAR_UPDATE, SALT_STATE_APPLY)),
    STACK_ENCRYPTION_KEYS(List.of(CUSTOM_JOB), Set.of(SKIP_SALT_UPDATE)),
    LUKS_VOLUME_PASSPHRASE(List.of(CUSTOM_JOB, SALT_STATE_APPLY)),
    USER_KEYPAIR(List.of(SALT_STATE_RUN, CUSTOM_JOB)),
    FREEIPA_ADMIN_PASSWORD(List.of(VAULT, FREEIPA_ADMIN_USER_PASSWORD, FREEIPA_DIRECTORY_MANAGER_PASSWORD, SALT_PILLAR_UPDATE), Set.of(SKIP_SALT_UPDATE)),
    FREEIPA_USERSYNC_USER_PASSWORD(List.of(VAULT, FREEIPA_USER_PASSWORD), Set.of(SKIP_SALT_UPDATE)),
    SALT_PASSWORD(List.of(VAULT, CUSTOM_JOB), Set.of(SKIP_SALT_UPDATE)),
    NGINX_CLUSTER_SSL_CERT_PRIVATE_KEY(List.of(SALT_STATE_APPLY, CUSTOM_JOB)),
    COMPUTE_MONITORING_CREDENTIALS(List.of(VAULT, CUSTOM_JOB)),
    // cluster related internal freeipa secrets
    FREEIPA_LDAP_BIND_PASSWORD(List.of(VAULT, FREEIPA_USER_PASSWORD), Set.of(SKIP_SALT_UPDATE, INTERNAL)),
    DEMO_SECRET(List.of(CUSTOM_JOB), Set.of(SKIP_SALT_UPDATE, INTERNAL)),
    DBUS_UMS_ACCESS_KEY(List.of(FREEIPA_UMS_DATABUS_CREDENTIAL, CUSTOM_JOB)),
    FREEIPA_KERBEROS_BIND_USER(List.of(VAULT, FREEIPA_USER_PASSWORD), Set.of(SKIP_SALT_UPDATE, INTERNAL));

    private final List<SecretRotationStep> steps;

    private final Set<SecretTypeFlag> flags;

    FreeIpaSecretType(List<SecretRotationStep> steps) {
        this.steps = steps;
        this.flags = Set.of();
    }

    FreeIpaSecretType(List<SecretRotationStep> steps, Set<SecretTypeFlag> flags) {
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
        return FreeIpaSecretType.class;
    }

    @Override
    public String value() {
        return name();
    }
}
