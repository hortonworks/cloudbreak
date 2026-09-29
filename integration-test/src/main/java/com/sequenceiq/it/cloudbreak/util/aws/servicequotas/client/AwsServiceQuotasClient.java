package com.sequenceiq.it.cloudbreak.util.aws.servicequotas.client;

import jakarta.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.it.cloudbreak.cloud.v4.aws.AwsProperties;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.servicequotas.ServiceQuotasClient;

@Service
public class AwsServiceQuotasClient {

    @Inject
    private AwsProperties awsProperties;

    public AwsServiceQuotasClient() {
    }

    public ServiceQuotasClient buildServiceQuotasClient() {
        String accessKeyId = awsProperties.getCredential().getAccessKeyId();
        String secretKey = awsProperties.getCredential().getSecretKey();
        String region = awsProperties.getRegion();

        AwsBasicCredentials awsCredentials = AwsBasicCredentials.create(accessKeyId, secretKey);

        return ServiceQuotasClient.builder()
                .credentialsProvider(StaticCredentialsProvider.create(awsCredentials))
                .region(Region.of(region))
                .build();
    }
}
