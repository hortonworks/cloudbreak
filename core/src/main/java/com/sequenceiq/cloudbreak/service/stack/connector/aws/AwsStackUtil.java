package com.sequenceiq.cloudbreak.service.stack.connector.aws;

import javax.inject.Inject;

import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.amazonaws.auth.BasicSessionCredentials;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.autoscaling.AmazonAutoScalingClient;
import com.amazonaws.services.cloudformation.AmazonCloudFormationClient;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.sns.AmazonSNSClient;
import com.sequenceiq.cloudbreak.domain.AwsCredential;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.service.credential.aws.CrossAccountCredentialsProvider;

@Component
public class AwsStackUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsStackUtil.class);

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
        LOGGER.debug("Amazon CloudFormation client successfully created.");
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
        LOGGER.debug("Amazon EC2 client successfully created.");
        return amazonEC2Client;
    }

    public AmazonAutoScalingClient createAutoScalingClient(Regions regions, AwsCredential credential) {
        BasicSessionCredentials basicSessionCredentials = credentialsProvider
                .retrieveSessionCredentials(CrossAccountCredentialsProvider.DEFAULT_SESSION_CREDENTIALS_DURATION,
                        credentialsProvider.getExternalId(), credential);
        AmazonAutoScalingClient amazonAutoScalingClient = new AmazonAutoScalingClient(basicSessionCredentials);
        amazonAutoScalingClient.setRegion(Region.getRegion(regions));
        LOGGER.debug("Amazon Autoscaling client successfully created.");
        return amazonAutoScalingClient;
    }

    public AmazonSNSClient createSnsClient(Regions region, AwsCredential credential) {
        BasicSessionCredentials basicSessionCredentials = credentialsProvider
                .retrieveSessionCredentials(CrossAccountCredentialsProvider.DEFAULT_SESSION_CREDENTIALS_DURATION,
                        credentialsProvider.getExternalId(), credential);
        AmazonSNSClient amazonSNSClient = new AmazonSNSClient(basicSessionCredentials);
        amazonSNSClient.setRegion(Region.getRegion(region));
        LOGGER.debug("Amazon SNS client successfully created.");
        return amazonSNSClient;
    }

    public String encode(String userData) {
        byte[] encoded = Base64.encodeBase64(userData.getBytes());
        return new String(encoded);
    }

    public void sleep(Stack stack, int duration) {
        try {
            Thread.sleep(duration);
        } catch (InterruptedException e) {
            LOGGER.info("Interrupted exception occured during polling.", e);
            Thread.currentThread().interrupt();
        }
    }

}
