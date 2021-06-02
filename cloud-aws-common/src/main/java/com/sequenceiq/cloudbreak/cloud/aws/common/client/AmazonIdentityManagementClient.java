package com.sequenceiq.cloudbreak.cloud.aws.common.client;

import com.amazonaws.services.identitymanagement.AmazonIdentityManagement;
import com.amazonaws.services.identitymanagement.model.GetInstanceProfileRequest;
import com.amazonaws.services.identitymanagement.model.GetInstanceProfileResult;
import com.amazonaws.services.identitymanagement.model.GetRoleRequest;
import com.amazonaws.services.identitymanagement.model.GetRoleResult;
import com.amazonaws.services.identitymanagement.model.ListInstanceProfilesRequest;
import com.amazonaws.services.identitymanagement.model.ListInstanceProfilesResult;
import com.amazonaws.services.identitymanagement.model.ListRolesRequest;
import com.amazonaws.services.identitymanagement.model.ListRolesResult;
import com.amazonaws.services.identitymanagement.model.SimulatePrincipalPolicyRequest;
import com.amazonaws.services.identitymanagement.model.SimulatePrincipalPolicyResult;

public class AmazonIdentityManagementClient extends AmazonClient {

    private final AmazonIdentityManagement client;

    public AmazonIdentityManagementClient(AmazonIdentityManagement client) {
        this.client = client;
    }

    public SimulatePrincipalPolicyResult simulatePrincipalPolicy(SimulatePrincipalPolicyRequest simulatePrincipalPolicyRequest) {
        return client.simulatePrincipalPolicy(simulatePrincipalPolicyRequest);
    }

    public GetInstanceProfileResult getInstanceProfile(GetInstanceProfileRequest instanceProfileRequest) {
        return client.getInstanceProfile(instanceProfileRequest);
    }

    public GetRoleResult getRole(GetRoleRequest roleRequest) {
        return client.getRole(roleRequest);
    }

    public ListRolesResult listRoles(ListRolesRequest listRolesRequest) {
        return client.listRoles(listRolesRequest);
    }

    public ListInstanceProfilesResult listInstanceProfiles(ListInstanceProfilesRequest listInstanceProfilesRequest) {
        return client.listInstanceProfiles(listInstanceProfilesRequest);
    }
}
