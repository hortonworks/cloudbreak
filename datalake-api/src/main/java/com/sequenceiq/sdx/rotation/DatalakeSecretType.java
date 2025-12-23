package com.sequenceiq.sdx.rotation;

import static com.sequenceiq.cloudbreak.rotation.CommonSecretRotationStep.CLOUDBREAK_ROTATE_POLLING;
import static com.sequenceiq.cloudbreak.rotation.CommonSecretRotationStep.FREEIPA_ROTATE_POLLING;
import static com.sequenceiq.cloudbreak.rotation.CommonSecretRotationStep.REDBEAMS_ROTATE_POLLING;
import static com.sequenceiq.cloudbreak.rotation.SecretTypeFlag.INTERNAL;
import static com.sequenceiq.cloudbreak.rotation.SecretTypeFlag.POST_FLOW;
import static com.sequenceiq.cloudbreak.rotation.SecretTypeFlag.SKIP_SALT_HIGHSTATE;
import static com.sequenceiq.cloudbreak.rotation.SecretTypeFlag.SKIP_SALT_UPDATE;
import static com.sequenceiq.cloudbreak.rotation.SecretTypeFlag.SKIP_STATUS_CHECK;

import java.util.List;
import java.util.Set;

import com.sequenceiq.cloudbreak.rotation.SecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.SecretType;
import com.sequenceiq.cloudbreak.rotation.SecretTypeFlag;

public enum DatalakeSecretType implements SecretType {

    CM_ADMIN_PASSWORD(List.of(CLOUDBREAK_ROTATE_POLLING), Set.of(SKIP_SALT_UPDATE)),
    EXTERNAL_DATABASE_ROOT_PASSWORD(List.of(REDBEAMS_ROTATE_POLLING, CLOUDBREAK_ROTATE_POLLING)),
    CM_DB_PASSWORD(List.of(CLOUDBREAK_ROTATE_POLLING)),
    USER_KEYPAIR(List.of(CLOUDBREAK_ROTATE_POLLING)),
    IDBROKER_CERT(List.of(CLOUDBREAK_ROTATE_POLLING)),
    GATEWAY_CERT(List.of(CLOUDBREAK_ROTATE_POLLING)),
    CM_SERVICES_DB_PASSWORD(List.of(CLOUDBREAK_ROTATE_POLLING)),
    CM_SERVICE_SHARED_DB(List.of(CLOUDBREAK_ROTATE_POLLING)),
    DEMO_SECRET(List.of(CLOUDBREAK_ROTATE_POLLING), Set.of(SKIP_SALT_UPDATE, INTERNAL)),
    SALT_BOOT_SECRETS(List.of(CLOUDBREAK_ROTATE_POLLING)),
    SALT_MASTER_KEY_PAIR(List.of(CLOUDBREAK_ROTATE_POLLING), Set.of(SKIP_SALT_UPDATE)),
    SALT_SIGN_KEY_PAIR(List.of(CLOUDBREAK_ROTATE_POLLING), Set.of(SKIP_SALT_UPDATE)),
    CM_INTERMEDIATE_CA_CERT(List.of(CLOUDBREAK_ROTATE_POLLING), Set.of(POST_FLOW)),
    LDAP_BIND_PASSWORD(List.of(CLOUDBREAK_ROTATE_POLLING)),
    SSSD_IPA_PASSWORD(List.of(FREEIPA_ROTATE_POLLING, CLOUDBREAK_ROTATE_POLLING), Set.of(SKIP_SALT_UPDATE)),
    STACK_ENCRYPTION_KEYS(List.of(CLOUDBREAK_ROTATE_POLLING), Set.of(SKIP_SALT_UPDATE)),
    LUKS_VOLUME_PASSPHRASE(List.of(CLOUDBREAK_ROTATE_POLLING)),
    SALT_PASSWORD(List.of(CLOUDBREAK_ROTATE_POLLING), Set.of(SKIP_SALT_UPDATE)),
    NGINX_CLUSTER_SSL_CERT_PRIVATE_KEY(List.of(CLOUDBREAK_ROTATE_POLLING)),
    COMPUTE_MONITORING_CREDENTIALS(List.of(CLOUDBREAK_ROTATE_POLLING)),
    EMBEDDED_DB_SSL_CERT(List.of(CLOUDBREAK_ROTATE_POLLING), Set.of(SKIP_STATUS_CHECK, SKIP_SALT_HIGHSTATE)),
    DBUS_UMS_ACCESS_KEY(List.of(CLOUDBREAK_ROTATE_POLLING));

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
