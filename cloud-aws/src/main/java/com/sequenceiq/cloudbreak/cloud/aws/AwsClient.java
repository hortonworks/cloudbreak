package com.sequenceiq.cloudbreak.cloud.aws;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.amazonaws.auth.BasicSessionCredentials;
import com.amazonaws.regions.RegionUtils;
import com.amazonaws.services.autoscaling.AmazonAutoScalingClient;
import com.amazonaws.services.cloudformation.AmazonCloudFormationClient;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.sequenceiq.cloudbreak.cloud.aws.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.cloud.event.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.event.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;

@Component
public class AwsClient {
    private static final String DEFAULT_REGION_NAME = "US_WEST_1";

    @Inject
    private AwsPlatformParameters awsPlatformParameters;

    @Inject
    private AwsSessionCredentialClient credentialClient;

    public AuthenticatedContext createAuthenticatedContext(CloudContext cloudContext, CloudCredential cloudCredential) {
        AuthenticatedContext authenticatedContext = new AuthenticatedContext(cloudContext, cloudCredential);
        authenticatedContext.putParameter(AmazonEC2Client.class, createAccess(authenticatedContext.getCloudCredential()));
        return authenticatedContext;
    }

    public AmazonEC2Client createAccess(CloudCredential credential) {
        return createAccess(new AwsCredentialView(credential), DEFAULT_REGION_NAME);
    }

    public AmazonEC2Client createAccess(AwsCredentialView awsCredential, String regionName) {
        BasicSessionCredentials basicSessionCredentials = credentialClient.retrieveCachedSessionCredentials(awsCredential);
        AmazonEC2Client amazonEC2Client = new AmazonEC2Client(basicSessionCredentials);
        amazonEC2Client.setRegion(RegionUtils.getRegion(awsPlatformParameters.regions().get(regionName)));
        return amazonEC2Client;
    }

    public AmazonCloudFormationClient createCloudFormationClient(AwsCredentialView awsCredential, String regionName) {
        BasicSessionCredentials basicSessionCredentials = credentialClient.retrieveCachedSessionCredentials(awsCredential);
        AmazonCloudFormationClient amazonCloudFormationClient = new AmazonCloudFormationClient(basicSessionCredentials);
        amazonCloudFormationClient.setRegion(RegionUtils.getRegion(awsPlatformParameters.regions().get(regionName)));
        return amazonCloudFormationClient;
    }

    public AmazonAutoScalingClient createAutoScalingClient(AwsCredentialView awsCredential, String regionName) {
        BasicSessionCredentials basicSessionCredentials = credentialClient.retrieveCachedSessionCredentials(awsCredential);
        AmazonAutoScalingClient amazonAutoScalingClient = new AmazonAutoScalingClient(basicSessionCredentials);
        amazonAutoScalingClient.setRegion(RegionUtils.getRegion(awsPlatformParameters.regions().get(regionName)));
        return amazonAutoScalingClient;
    }

    public String getCbName(String groupName, Long number) {
        return String.format("%s%s", groupName, number);
    }

    public String getKeyPairName(AuthenticatedContext ac) {
        return String.format("%s%s%s%s", ac.getCloudCredential().getName(), ac.getCloudCredential().getId(),
                ac.getCloudContext().getName(), ac.getCloudContext().getId());
    }


}
