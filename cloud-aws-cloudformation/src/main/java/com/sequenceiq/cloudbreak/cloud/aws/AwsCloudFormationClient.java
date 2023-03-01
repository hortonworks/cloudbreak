package com.sequenceiq.cloudbreak.cloud.aws;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.google.common.annotations.VisibleForTesting;
import com.sequenceiq.cloudbreak.cloud.aws.client.AmazonAutoScalingClient;
import com.sequenceiq.cloudbreak.cloud.aws.client.AmazonCloudFormationClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.AwsClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.service.Retry;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.autoscaling.AutoScalingClient;
import software.amazon.awssdk.services.autoscaling.AutoScalingClientBuilder;
import software.amazon.awssdk.services.cloudformation.CloudFormationClient;
import software.amazon.awssdk.services.cloudformation.CloudFormationClientBuilder;

@Component
public class AwsCloudFormationClient extends AwsClient {

    @Inject
    private Retry retry;

    public AmazonCloudFormationClient createCloudFormationClient(AwsCredentialView awsCredential, String regionName) {
        CloudFormationClient cloudFormationClient = createCloudFormation(awsCredential, regionName);
        return new AmazonCloudFormationClient(proxy(cloudFormationClient, awsCredential, regionName), retry);
    }

    @VisibleForTesting
    CloudFormationClient createCloudFormation(AwsCredentialView awsCredential, String regionName) {
        CloudFormationClientBuilder cloudFormationClientBuilder = CloudFormationClient.builder()
                .credentialsProvider(getCredentialProvider(awsCredential))
                .region(Region.of(regionName))
                .overrideConfiguration(getDefaultClientConfiguration());
        setupFipsEndpointIfNecessary(CloudFormationClient.SERVICE_NAME, regionName, awsCredential).ifPresent(cloudFormationClientBuilder::endpointOverride);
        return cloudFormationClientBuilder.build();
    }

    public AmazonAutoScalingClient createAutoScalingClient(AwsCredentialView awsCredential, String regionName) {
        AutoScalingClientBuilder autoScalingClientBuilder = AutoScalingClient.builder()
                .credentialsProvider(getCredentialProvider(awsCredential))
                .region(Region.of(regionName))
                .overrideConfiguration(getDefaultClientConfiguration());
        setupFipsEndpointIfNecessary(AutoScalingClient.SERVICE_NAME, regionName, awsCredential).ifPresent(autoScalingClientBuilder::endpointOverride);
        return new AmazonAutoScalingClient(proxy(autoScalingClientBuilder.build(), awsCredential, regionName), retry);
    }
}
