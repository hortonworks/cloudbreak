package com.sequenceiq.cloudbreak.auth.altus.service;

public enum UmsResourceRole {
    OWNER("Owner"),
    ENVIRONMENT_ADMIN("EnvironmentAdmin"),
    ENVIRONMENT_USER("EnvironmentUser"),
    WXM_CLUSTER_ADMIN("WXMClusterAdmin");

    private String resourceRoleName;

    UmsResourceRole(String resourceRoleName) {
        this.resourceRoleName = resourceRoleName;
    }

    public String getResourceRoleName() {
        return resourceRoleName;
    }
}
