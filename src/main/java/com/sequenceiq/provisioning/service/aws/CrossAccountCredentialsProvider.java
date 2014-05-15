package com.sequenceiq.provisioning.service.aws;

import org.springframework.stereotype.Component;

import com.amazonaws.auth.BasicSessionCredentials;
import com.amazonaws.services.securitytoken.AWSSecurityTokenServiceClient;
import com.amazonaws.services.securitytoken.model.AssumeRoleRequest;
import com.amazonaws.services.securitytoken.model.AssumeRoleResult;

/**
 * Provides temporary session credentials for cross account requests. The assume
 * role request is made with basic credentials found in the default provider
 * chain (e.g.: environment variables). The assume role request must be made
 * with IAM user credentials, and the user must have a policy assigned that
 * allows her to call sts:AssumeRole. To be able to make cross-account requests
 * on a specific account, an IAM Role must be created on that account and it
 * must be configured with the accountId of the trusted partner.
 */
@Component
public class CrossAccountCredentialsProvider {

    public BasicSessionCredentials retrieveSessionCredentials(int durationInSeconds, String externalId, String roleARN) {
        AWSSecurityTokenServiceClient client = new AWSSecurityTokenServiceClient();
        AssumeRoleRequest assumeRoleRequest = new AssumeRoleRequest()
                .withDurationSeconds(durationInSeconds)
                .withExternalId(externalId)
                .withRoleArn(roleARN)
                .withRoleSessionName("hadoop-provisioning");
        AssumeRoleResult result = client.assumeRole(assumeRoleRequest);
        return new BasicSessionCredentials(
                result.getCredentials().getAccessKeyId(),
                result.getCredentials().getSecretAccessKey(),
                result.getCredentials().getSessionToken());
    }
}
