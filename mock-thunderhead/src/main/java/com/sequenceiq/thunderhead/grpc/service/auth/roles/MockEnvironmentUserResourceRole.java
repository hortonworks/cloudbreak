package com.sequenceiq.thunderhead.grpc.service.auth.roles;

import java.util.Set;

import org.springframework.stereotype.Component;

import com.google.common.collect.ImmutableSet;
import com.sequenceiq.thunderhead.grpc.service.auth.MockPermissionControl;

@Component
public class MockEnvironmentUserResourceRole {

    // List of users that should be considered as environment users
    private static final Set<String> USERS = ImmutableSet.of("envuser0", "envuser1");

    private static final Set<String> WORKLOAD_ADMIN_GROUPS = ImmutableSet.of("_c_environments_accessenvironment");

    // List of rights that should be considered as environment user resource role rights
    private static final Set<String> ENVIROMENT_USER_RESOURCE_ROLE_RIGHTS = Set.of(
            "compute/describe",
            "compute/get",
            "compute/list",
            "datahub/describeDatahub",
            "datalake/describeDatalake",
            "datalake/describeDetailedDatalake",
            "environments/accessEnvironment",
            "environments/describeCredential",
            "environments/describeEnvironment",
            "environments/getAutomatedSyncEnvironmentStatus",
            "environments/getFreeipaOperationStatus",
            "environments/getKeytab",
            "environments/read",
            "environments/setPassword",
            "workloadconnectivity/getConnectivityStatus"
    );

    public boolean hasMatchingUser(String crn) {
        // last segment of the crn is the username in case of mock
        try {
            return USERS.contains(crn.substring(crn.lastIndexOf(":") + 1));
        } catch (Exception e) {
            // in case of invalid crn this mock resource role should not be applied,
            // we can completely ignore this since it might be a test that contains invalid crn
            return false;
        }
    }

    public Set<String> getUserNames() {
        return USERS;
    }

    public Set<String> getWorkloadAdministrationGroupNames() {
        return WORKLOAD_ADMIN_GROUPS;
    }

    public MockPermissionControl hasRight(String userCrn, String right) {
        MockPermissionControl permissionControl = MockPermissionControl.NOT_IMPLEMENTED;
        if (hasMatchingUser(userCrn)) {
            if (ENVIROMENT_USER_RESOURCE_ROLE_RIGHTS.contains(right)) {
                permissionControl = MockPermissionControl.APPROVED;
            } else {
                permissionControl = MockPermissionControl.DENIED;
            }
        }
        return permissionControl;
    }
}
