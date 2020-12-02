package com.sequenceiq.cloudbreak.auth.altus.service;

import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.cloudbreak.auth.altus.CrnResourceDescriptor;

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
        return Crn.builder(CrnResourceDescriptor.RESOURCE_ROLE)
                .setOldPartition()
                .setAccountId(ACCOUNT_IN_IAM_CRNS)
                .setResource(resourceRoleName)
                .build();
    }

    public static Crn getRoleCrn(String roleName) {
        return Crn.builder(CrnResourceDescriptor.ROLE)
                .setOldPartition()
                .setAccountId(ACCOUNT_IN_IAM_CRNS)
                .setResource(roleName)
                .build();
    }
}
