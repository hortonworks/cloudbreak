package com.sequenceiq.cloudbreak.service.stack.connector.aws;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.amazonaws.auth.BasicSessionCredentials;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.autoscaling.AmazonAutoScalingClient;
import com.amazonaws.services.cloudformation.AmazonCloudFormationClient;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.sequenceiq.cloudbreak.controller.validation.StackParam;
import com.sequenceiq.cloudbreak.domain.AwsCredential;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.service.credential.aws.CrossAccountCredentialsProvider;

@Component
public class AwsStackUtil {

    @Inject
    private CrossAccountCredentialsProvider credentialsProvider;

    public AmazonCloudFormationClient createCloudFormationClient(Stack stack) {
        return createCloudFormationClient(Regions.valueOf(stack.getRegion()), (AwsCredential) stack.getCredential());
    }

    public AmazonCloudFormationClient createCloudFormationClient(Regions regions, AwsCredential credential) {
        BasicSessionCredentials basicSessionCredentials = credentialsProvider
                .retrieveSessionCredentials(CrossAccountCredentialsProvider.DEFAULT_SESSION_CREDENTIALS_DURATION,
                        credentialsProvider.getExternalId(), credential);
        AmazonCloudFormationClient amazonCloudFormationClient = new AmazonCloudFormationClient(basicSessionCredentials);
        amazonCloudFormationClient.setRegion(Region.getRegion(regions));
        return amazonCloudFormationClient;
    }

    public AmazonEC2Client createEC2Client(Stack stack) {
        return createEC2Client(Regions.valueOf(stack.getRegion()), (AwsCredential) stack.getCredential());
    }

    public AmazonEC2Client createEC2Client(Regions regions, AwsCredential credential) {
        BasicSessionCredentials basicSessionCredentials = credentialsProvider
                .retrieveSessionCredentials(CrossAccountCredentialsProvider.DEFAULT_SESSION_CREDENTIALS_DURATION,
                        credentialsProvider.getExternalId(), credential);
        AmazonEC2Client amazonEC2Client = new AmazonEC2Client(basicSessionCredentials);
        amazonEC2Client.setRegion(Region.getRegion(regions));
        return amazonEC2Client;
    }

    public AmazonAutoScalingClient createAutoScalingClient(Stack stack) {
        return createAutoScalingClient(Regions.valueOf(stack.getRegion()), (AwsCredential) stack.getCredential());
    }

    public AmazonAutoScalingClient createAutoScalingClient(Regions regions, AwsCredential credential) {
        BasicSessionCredentials basicSessionCredentials = credentialsProvider
                .retrieveSessionCredentials(CrossAccountCredentialsProvider.DEFAULT_SESSION_CREDENTIALS_DURATION,
                        credentialsProvider.getExternalId(), credential);
        AmazonAutoScalingClient amazonAutoScalingClient = new AmazonAutoScalingClient(basicSessionCredentials);
        amazonAutoScalingClient.setRegion(Region.getRegion(regions));
        return amazonAutoScalingClient;
    }

    public boolean areDedicatedInstancesRequested(Stack stack) {
        boolean result = false;
        if (isDedicatedInstancesParamExistAndTrue(stack)) {
            result = true;
        }
        return result;
    }

    private boolean isDedicatedInstancesParamExistAndTrue(Stack stack) {
        return stack.getParameters().containsKey(StackParam.DEDICATED_INSTANCES.getName())
                && Boolean.valueOf(stack.getParameters().get(StackParam.DEDICATED_INSTANCES.getName()));
    }
}
