package com.sequenceiq.cloudbreak.auth.altus.service;

public enum UmsResourceRole {
    DATAHUB_CREATOR("DataHubCreator"),
    DATAHUB_ADMIN("DataHubAdmin"),
    DATAHUB_USER("DataHubUser"),
    DATA_STEWARD("DataSteward"),
    SHARED_RESOURCE_USER("SharedResourceUser"),
    IAM_GROUP_ADMIN("IamGroupAdmin"),
    ENVIRONMENT_PRIVILEGED_USER("EnvironmentPrivilegedUser"),
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
