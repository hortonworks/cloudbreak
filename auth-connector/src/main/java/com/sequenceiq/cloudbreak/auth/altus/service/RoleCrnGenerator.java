package com.sequenceiq.cloudbreak.auth.altus.service;

import com.sequenceiq.cloudbreak.auth.altus.Crn;

public class RoleCrnGenerator {

    private static final String ACCOUNT_IN_IAM_CRNS = "altus";

    private RoleCrnGenerator() {

    }

    /**
     * Get built-in Dbus uploader role
     * Partition and region is hard coded right now, if it will change use the same as the user crn
     */
    public static String getBuiltInDatabusRoleCrn() {
        return getRoleCrn("DbusUploader").toString();
    }

    public static String getBuiltInOwnerResourceRoleCrn() {
        return getResourceRoleCrn("Owner").toString();
    }

    public static String getBuiltInEnvironmentAdminResourceRoleCrn() {
        return getResourceRoleCrn("EnvironmentAdmin").toString();
    }

    public static Crn getResourceRoleCrn(String resourceRoleName) {
        return getBaseIamCrnBuilder()
                .setResourceType(Crn.ResourceType.RESOURCE_ROLE)
                .setResource(resourceRoleName)
                .build();
    }

    public static Crn getRoleCrn(String roleName) {
        return getBaseIamCrnBuilder()
                .setResourceType(Crn.ResourceType.ROLE)
                .setResource(roleName)
                .build();
    }

    private static Crn.Builder getBaseIamCrnBuilder() {
        return Crn.builder()
                .setPartition(Crn.Partition.ALTUS)
                .setAccountId(ACCOUNT_IN_IAM_CRNS)
                .setService(Crn.Service.IAM);
    }
}
