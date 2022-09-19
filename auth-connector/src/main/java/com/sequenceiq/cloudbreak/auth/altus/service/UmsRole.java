package com.sequenceiq.cloudbreak.auth.altus.service;

public enum UmsRole {
    DBUS_UPLOADER("DbusUploader"),
    COMPUTE_METRICS_PUBLISHER("ComputeMetricsPublisher");

    private String roleName;

    UmsRole(String roleName) {
        this.roleName = roleName;
    }

    public String getRoleName() {
        return roleName;
    }
}
