package com.sequenceiq.cloudbreak.auth.altus;

public enum UmsRight {
    ENVIRONMENT_ACCESS("environments/accessEnvironment"),
    CLOUDER_MANAGER_ADMIN("environments/adminClouderaManager"),
    RANGER_ADMIN("environments/adminRanger"),
    KNOX_ADMIN("environments/adminKnox"),
    ZEPPELIN_ADMIN("environments/adminZeppelin"),
    NIFI_ADMIN("datahub/adminNiFi"),
    NIFI_REGISTRY_ADMIN("datahub/adminNiFiRegistry");

    private final String right;

    UmsRight(String right) {
        this.right = right;
    }

    public String getRight() {
        return right;
    }
}
