package com.sequenceiq.cloudbreak.cloud.aws.common.client;

import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.model.AssumeRoleRequest;
import software.amazon.awssdk.services.sts.model.AssumeRoleResponse;
import software.amazon.awssdk.services.sts.model.DecodeAuthorizationMessageRequest;
import software.amazon.awssdk.services.sts.model.DecodeAuthorizationMessageResponse;
import software.amazon.awssdk.services.sts.model.GetCallerIdentityRequest;
import software.amazon.awssdk.services.sts.model.GetCallerIdentityResponse;

public class AmazonSecurityTokenServiceClient extends AmazonClient {

    private final StsClient client;

    public AmazonSecurityTokenServiceClient(StsClient client) {
        this.client = client;
    }

    public GetCallerIdentityResponse getCallerIdentity(GetCallerIdentityRequest getCallerIdentityRequest) {
        return client.getCallerIdentity(getCallerIdentityRequest);
    }

    public DecodeAuthorizationMessageResponse decodeAuthorizationMessage(DecodeAuthorizationMessageRequest decodeAuthorizationMessageRequest) {
        return client.decodeAuthorizationMessage(decodeAuthorizationMessageRequest);
    }

    public AssumeRoleResponse assumeRole(AssumeRoleRequest assumeRoleRequest) {
        return client.assumeRole(assumeRoleRequest);
    }
}
