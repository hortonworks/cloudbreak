package com.sequenceiq.cloudbreak.cloud.aws.common.client;

import software.amazon.awssdk.services.iam.IamClient;
import software.amazon.awssdk.services.iam.model.GetInstanceProfileRequest;
import software.amazon.awssdk.services.iam.model.GetInstanceProfileResponse;
import software.amazon.awssdk.services.iam.model.GetRoleRequest;
import software.amazon.awssdk.services.iam.model.GetRoleResponse;
import software.amazon.awssdk.services.iam.model.ListInstanceProfilesRequest;
import software.amazon.awssdk.services.iam.model.ListInstanceProfilesResponse;
import software.amazon.awssdk.services.iam.model.ListRolesRequest;
import software.amazon.awssdk.services.iam.model.ListRolesResponse;
import software.amazon.awssdk.services.iam.model.SimulatePrincipalPolicyRequest;
import software.amazon.awssdk.services.iam.model.SimulatePrincipalPolicyResponse;

public class AmazonIdentityManagementClient extends AmazonClient {

    private final IamClient client;

    public AmazonIdentityManagementClient(IamClient client) {
        this.client = client;
    }

    public SimulatePrincipalPolicyResponse simulatePrincipalPolicy(SimulatePrincipalPolicyRequest simulatePrincipalPolicyRequest) {
        return client.simulatePrincipalPolicy(simulatePrincipalPolicyRequest);
    }

    public GetInstanceProfileResponse getInstanceProfile(GetInstanceProfileRequest instanceProfileRequest) {
        return client.getInstanceProfile(instanceProfileRequest);
    }

    public GetRoleResponse getRole(GetRoleRequest roleRequest) {
        return client.getRole(roleRequest);
    }

    public ListRolesResponse listRoles(ListRolesRequest listRolesRequest) {
        return client.listRoles(listRolesRequest);
    }

    public ListInstanceProfilesResponse listInstanceProfiles(ListInstanceProfilesRequest listInstanceProfilesRequest) {
        return client.listInstanceProfiles(listInstanceProfilesRequest);
    }
}
