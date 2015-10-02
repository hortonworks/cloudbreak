package com.sequenceiq.cloudbreak.cloud.aws;

import static com.sequenceiq.cloudbreak.EnvironmentVariableConfig.CB_AWS_EXTERNAL_ID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import com.amazonaws.services.autoscaling.AmazonAutoScalingClient;
import com.amazonaws.services.cloudformation.AmazonCloudFormationClient;
import com.amazonaws.auth.BasicSessionCredentials;
import com.amazonaws.regions.RegionUtils;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.securitytoken.AWSSecurityTokenServiceClient;
import com.amazonaws.services.securitytoken.model.AssumeRoleRequest;
import com.amazonaws.services.securitytoken.model.AssumeRoleResult;
import com.sequenceiq.cloudbreak.cloud.aws.cache.CachingConfig;
import com.sequenceiq.cloudbreak.cloud.aws.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.cloud.event.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.event.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;

@Component
public class AwsClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(AwsClient.class);

    private static final int DEFAULT_SESSION_CREDENTIALS_DURATION = 3600;
    private static final String DEFAULT_REGION_NAME = "us-west-1";

    @Value("${cb.aws.external.id:" + CB_AWS_EXTERNAL_ID + "}")
    private String externalId;

    public AuthenticatedContext createAuthenticatedContext(CloudContext cloudContext, CloudCredential cloudCredential) {
        AuthenticatedContext authenticatedContext = new AuthenticatedContext(cloudContext, cloudCredential);
        authenticatedContext.putParameter(AmazonEC2Client.class, createAccess(authenticatedContext.getCloudCredential(), cloudContext.getRegion()));
        return authenticatedContext;
    }

    public AmazonEC2Client createAccess(CloudCredential credential) {
        return createAccess(credential, DEFAULT_REGION_NAME);
    }

    public AmazonEC2Client createAccess(CloudCredential credential, String regionName) {
        AwsCredentialView awsCredential = new AwsCredentialView(credential);
        return createAccess(awsCredential, regionName);
    }

    public AmazonEC2Client createAccess(AwsCredentialView armCredential, String regionName) {
        BasicSessionCredentials basicSessionCredentials = retrieveSessionCredentials(armCredential);
        AmazonEC2Client amazonEC2Client = new AmazonEC2Client(basicSessionCredentials);
        amazonEC2Client.setRegion(RegionUtils.getRegion(regionName));
        return amazonEC2Client;
    }

    public AmazonCloudFormationClient createCloudFormationClient(CloudCredential credential) {
        AwsCredentialView awsCredential = new AwsCredentialView(credential);
        return createCloudFormationClient(awsCredential);
    }

    public AmazonCloudFormationClient createCloudFormationClient(AwsCredentialView armCredential) {
        /*
        BasicSessionCredentials basicSessionCredentials = credentialsProvider
                .retrieveSessionCredentials(CrossAccountCredentialsProvider.DEFAULT_SESSION_CREDENTIALS_DURATION,
                        credentialsProvider.getExternalId(), credential);
        AmazonCloudFormationClient amazonCloudFormationClient = new AmazonCloudFormationClient(basicSessionCredentials);
        amazonCloudFormationClient.setRegion(Region.getRegion(regions));*/
        return null;
    }

    public AmazonAutoScalingClient createAutoScalingClient(CloudCredential credential) {
        AwsCredentialView awsCredential = new AwsCredentialView(credential);
        return createAutoScalingClient(awsCredential);
    }

    public AmazonAutoScalingClient createAutoScalingClient(AwsCredentialView armCredential) {
        /*BasicSessionCredentials basicSessionCredentials = credentialsProvider
                .retrieveSessionCredentials(CrossAccountCredentialsProvider.DEFAULT_SESSION_CREDENTIALS_DURATION,
                        credentialsProvider.getExternalId(), credential);
        AmazonAutoScalingClient amazonAutoScalingClient = new AmazonAutoScalingClient(basicSessionCredentials);
        amazonAutoScalingClient.setRegion(Region.getRegion(regions));*/
        return null;
    }

    @Cacheable(CachingConfig.TEMPORARY_AWS_CREDENTIAL_CACHE)
    public BasicSessionCredentials retrieveCachedSessionCredentials(AwsCredentialView awsCredential) {
        return retrieveSessionCredentials(awsCredential);
    }

    public BasicSessionCredentials retrieveSessionCredentials(AwsCredentialView awsCredential) {
        LOGGER.debug("retrieving sesson credential");
        AWSSecurityTokenServiceClient client = new AWSSecurityTokenServiceClient();
        AssumeRoleRequest assumeRoleRequest = new AssumeRoleRequest()
                .withDurationSeconds(DEFAULT_SESSION_CREDENTIALS_DURATION)
                .withExternalId(externalId)
                .withRoleArn(awsCredential.getRoleArn())
                .withRoleSessionName("hadoop-provisioning");
        AssumeRoleResult result = client.assumeRole(assumeRoleRequest);

        return new BasicSessionCredentials(
                result.getCredentials().getAccessKeyId(),
                result.getCredentials().getSecretAccessKey(),
                result.getCredentials().getSessionToken());
    }

}
