package com.sequenceiq.it.cloudbreak.util.aws.amazonrgt.client;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.it.cloudbreak.cloud.v4.aws.AwsProperties;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.resourcegroupstaggingapi.ResourceGroupsTaggingApiClient;

@Service
public class AwsResourceGroupTaggingClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(AwsResourceGroupTaggingClient.class);

    @Inject
    private AwsProperties awsProperties;

    public AwsResourceGroupTaggingClient() {
    }

    public ResourceGroupsTaggingApiClient buildTaggingClient() {
        String accessKeyId = awsProperties.getCredential().getAccessKeyId();
        String secretKey = awsProperties.getCredential().getSecretKey();
        String region = awsProperties.getRegion();

        AwsBasicCredentials awsCredentials = AwsBasicCredentials.create(accessKeyId, secretKey);

        return ResourceGroupsTaggingApiClient.builder()
                .credentialsProvider(StaticCredentialsProvider.create(awsCredentials))
                .region(Region.of(region))
                .build();
    }
}
