package com.sequenceiq.cloudbreak.cloud.aws;

import org.springframework.stereotype.Component;

import com.amazonaws.services.autoscaling.AmazonAutoScalingClient;
import com.amazonaws.services.cloudformation.AmazonCloudFormationClient;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.sequenceiq.cloudbreak.cloud.aws.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.cloud.event.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.event.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;

@Component
public class AwsClient {

    public AuthenticatedContext createAuthenticatedContext(CloudContext cloudContext, CloudCredential cloudCredential) {
        AuthenticatedContext authenticatedContext = new AuthenticatedContext(cloudContext, cloudCredential);
        authenticatedContext.putParameter(AmazonEC2Client.class, createAccess(authenticatedContext.getCloudCredential()));
        return authenticatedContext;
    }

    public AmazonEC2Client createAccess(CloudCredential credential) {
        AwsCredentialView awsCredential = new AwsCredentialView(credential);
        return createAccess(awsCredential);
    }

    public AmazonEC2Client createAccess(AwsCredentialView armCredential) {
       /* BasicSessionCredentials basicSessionCredentials = credentialsProvider
                .retrieveSessionCredentials(CrossAccountCredentialsProvider.DEFAULT_SESSION_CREDENTIALS_DURATION,
                        credentialsProvider.getExternalId(), credential);*/
        //AmazonEC2Client amazonEC2Client = new AmazonEC2Client(basicSessionCredentials);
        //amazonEC2Client.setRegion(Region.getRegion(regions));
        return null;
    }

    public AmazonCloudFormationClient createCloudFormationClient(CloudCredential credential) {
        AwsCredentialView awsCredential = new AwsCredentialView(credential);
        return createCloudFormationClient(awsCredential);
    }

    public AmazonCloudFormationClient createCloudFormationClient(AwsCredentialView armCredential) {
       /* BasicSessionCredentials basicSessionCredentials = credentialsProvider
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

}
