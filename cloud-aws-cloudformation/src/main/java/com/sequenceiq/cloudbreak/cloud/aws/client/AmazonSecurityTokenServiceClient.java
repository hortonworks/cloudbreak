package com.sequenceiq.cloudbreak.cloud.aws.client;

import com.amazonaws.services.securitytoken.AWSSecurityTokenService;
import com.amazonaws.services.securitytoken.model.AssumeRoleRequest;
import com.amazonaws.services.securitytoken.model.AssumeRoleResult;
import com.amazonaws.services.securitytoken.model.DecodeAuthorizationMessageRequest;
import com.amazonaws.services.securitytoken.model.DecodeAuthorizationMessageResult;
import com.amazonaws.services.securitytoken.model.GetCallerIdentityRequest;
import com.amazonaws.services.securitytoken.model.GetCallerIdentityResult;

public class AmazonSecurityTokenServiceClient extends AmazonClient {

    private final AWSSecurityTokenService client;

    public AmazonSecurityTokenServiceClient(AWSSecurityTokenService client) {
        this.client = client;
    }

    public GetCallerIdentityResult getCallerIdentity(GetCallerIdentityRequest getCallerIdentityRequest) {
        return client.getCallerIdentity(getCallerIdentityRequest);
    }

    public DecodeAuthorizationMessageResult decodeAuthorizationMessage(DecodeAuthorizationMessageRequest decodeAuthorizationMessageRequest) {
        return client.decodeAuthorizationMessage(decodeAuthorizationMessageRequest);
    }

    public AssumeRoleResult assumeRole(AssumeRoleRequest assumeRoleRequest) {
        return client.assumeRole(assumeRoleRequest);
    }
}
