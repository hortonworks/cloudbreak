package com.sequenceiq.cloudbreak.cloud.aws;

import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.isNoneEmpty;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.RegionUtils;
import com.amazonaws.services.autoscaling.AmazonAutoScalingClient;
import com.amazonaws.services.cloudformation.AmazonCloudFormationClient;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.sequenceiq.cloudbreak.cloud.aws.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.event.credential.CredentialVerificationException;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;

@Component
public class AwsClient {
    private static final String DEFAULT_REGION_NAME = "us-west-1";

    @Inject
    private AwsPlatformParameters awsPlatformParameters;

    @Inject
    private AwsSessionCredentialClient credentialClient;

    public AuthenticatedContext createAuthenticatedContext(CloudContext cloudContext, CloudCredential cloudCredential) {
        AuthenticatedContext authenticatedContext = new AuthenticatedContext(cloudContext, cloudCredential);
        try {
            authenticatedContext.putParameter(AmazonEC2Client.class, createAccess(authenticatedContext.getCloudCredential()));
        } catch (AmazonServiceException e) {
            throw new CredentialVerificationException(e.getErrorMessage(), e);
        }
        return authenticatedContext;
    }

    public AmazonEC2Client createAccess(CloudCredential credential) {
        return createAccess(new AwsCredentialView(credential), DEFAULT_REGION_NAME);
    }

    public AmazonEC2Client createAccess(AwsCredentialView awsCredential, String regionName) {
        AmazonEC2Client client = isRoleAssumeRequired(awsCredential)
                ? new AmazonEC2Client(credentialClient.retrieveCachedSessionCredentials(awsCredential))
                : new AmazonEC2Client(createAwsCredentials(awsCredential));
        client.setRegion(RegionUtils.getRegion(regionName));
        return client;
    }

    public AmazonCloudFormationClient createCloudFormationClient(AwsCredentialView awsCredential, String regionName) {
        AmazonCloudFormationClient client = isRoleAssumeRequired(awsCredential)
                ? new AmazonCloudFormationClient(credentialClient.retrieveCachedSessionCredentials(awsCredential))
                : new AmazonCloudFormationClient(createAwsCredentials(awsCredential));
        client.setRegion(RegionUtils.getRegion(regionName));
        return client;
    }

    public AmazonAutoScalingClient createAutoScalingClient(AwsCredentialView awsCredential, String regionName) {
        AmazonAutoScalingClient client = isRoleAssumeRequired(awsCredential)
                ? new AmazonAutoScalingClient(credentialClient.retrieveCachedSessionCredentials(awsCredential))
                : new AmazonAutoScalingClient(createAwsCredentials(awsCredential));
        client.setRegion(RegionUtils.getRegion(regionName));
        return client;
    }

    public String getCbName(String groupName, Long number) {
        return String.format("%s%s", groupName, number);
    }

    public String getKeyPairName(AuthenticatedContext ac) {
        return String.format("%s%s%s%s", ac.getCloudCredential().getName(), ac.getCloudCredential().getId(),
                ac.getCloudContext().getName(), ac.getCloudContext().getId());
    }

    public void checkAwsEnvironmentVariables(CloudCredential credential) {
        AwsCredentialView awsCredential = new AwsCredentialView(credential);
        if (isRoleAssumeRequired(awsCredential)) {
            if (isEmpty(System.getenv("AWS_ACCESS_KEY_ID")) || isEmpty(System.getenv("AWS_SECRET_ACCESS_KEY"))) {
                throw new CloudConnectorException("For this operation the 'AWS_ACCESS_KEY_ID' and 'AWS_SECRET_ACCESS_KEY' environment variables must be set!");
            }
        }
    }

    private boolean isRoleAssumeRequired(AwsCredentialView awsCredential) {
        return isNoneEmpty(awsCredential.getRoleArn()) && isEmpty(awsCredential.getAccessKey()) && isEmpty(awsCredential.getSecretKey());
    }

    private BasicAWSCredentials createAwsCredentials(AwsCredentialView credentialView) {
        String accessKey = credentialView.getAccessKey();
        String secretKey = credentialView.getSecretKey();
        if (isEmpty(accessKey) || isEmpty(secretKey)) {
            throw new CloudConnectorException("Missing access or secret key from the credential.");
        }
        return new BasicAWSCredentials(accessKey, secretKey);
    }
}
