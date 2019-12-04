package com.sequenceiq.cloudbreak.cloud.aws.client;

import com.amazonaws.services.identitymanagement.AmazonIdentityManagement;
import com.amazonaws.services.identitymanagement.model.SimulatePrincipalPolicyRequest;
import com.amazonaws.services.identitymanagement.model.SimulatePrincipalPolicyResult;
import com.sequenceiq.cloudbreak.service.Retry;

public class AmazonIdentityManagementRetryClient extends AmazonRetryClient {

    private final AmazonIdentityManagement client;

    private final Retry retry;

    public AmazonIdentityManagementRetryClient(AmazonIdentityManagement client, Retry retry) {
        this.client = client;
        this.retry = retry;
    }

    public SimulatePrincipalPolicyResult simulatePrincipalPolicy(SimulatePrincipalPolicyRequest request) {
        return retry.testWith1SecDelayMax5Times(() -> mapThrottlingError(() -> client.simulatePrincipalPolicy(request)));
    }

}