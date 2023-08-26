package com.sequenceiq.cloudbreak.auth.altus;

import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_ENVIRONMENT_PRIVILEGED_USER;

import java.util.Optional;

import com.sequenceiq.cloudbreak.auth.altus.model.Entitlement;

public enum UmsVirtualGroupRight {
    ENVIRONMENT_ACCESS("environments/accessEnvironment"),
    CLOUDER_MANAGER_ADMIN("environments/adminClouderaManager"),
    RANGER_ADMIN("environments/adminRanger"),
    KNOX_ADMIN("environments/adminKnox"),
    ZEPPELIN_ADMIN("environments/adminZeppelin"),
    NIFI_ADMIN("datahub/adminNiFi"),
    NIFI_REGISTRY_ADMIN("datahub/adminNiFiRegistry"),
    EFM_ADMIN("datahub/adminEfm"),
    HBASE_ADMIN("datahub/adminHBase"),
    ALLOW_PRIVILEGED_OS_OPERATIONS("environments/allowPrivilegedOSOperations", CDP_ENVIRONMENT_PRIVILEGED_USER);

    private final String right;

    private final Optional<Entitlement> entitlement;

    UmsVirtualGroupRight(String right) {
        this.right = right;
        this.entitlement = Optional.empty();
    }

    UmsVirtualGroupRight(String right, Entitlement entitlement) {
        this.right = right;
        this.entitlement = Optional.of(entitlement);
    }

    public String getRight() {
        return right;
    }

    public Optional<Entitlement> getEntitlement() {
        return entitlement;
    }
}
