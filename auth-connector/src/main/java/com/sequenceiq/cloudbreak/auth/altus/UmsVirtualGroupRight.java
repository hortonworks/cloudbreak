package com.sequenceiq.cloudbreak.auth.altus;

public enum UmsVirtualGroupRight {
    ENVIRONMENT_ACCESS("environments/accessEnvironment"),
    CLOUDER_MANAGER_ADMIN("environments/adminClouderaManager"),
    RANGER_ADMIN("environments/adminRanger"),
    KNOX_ADMIN("environments/adminKnox"),
    ZEPPELIN_ADMIN("environments/adminZeppelin"),
    NIFI_ADMIN("datahub/adminNiFi"),
    NIFI_REGISTRY_ADMIN("datahub/adminNiFiRegistry"),
    HBASE_ADMIN("datahub/adminHBase"),
    ALLOW_PRIVILEGED_OS_OPERATIONS("environments/allowPrivilegedOSOperations");

    private final String right;

    UmsVirtualGroupRight(String right) {
        this.right = right;
    }

    public String getRight() {
        return right;
    }
}
