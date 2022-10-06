package com.sequenceiq.it.cloudbreak.util.aws.amazoncf.client;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.cloudformation.AmazonCloudFormation;
import com.amazonaws.services.cloudformation.AmazonCloudFormationClientBuilder;
import com.sequenceiq.it.cloudbreak.cloud.v4.aws.AwsProperties;

@Service
public class CfClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(CfClient.class);

    @Inject
    private AwsProperties awsProperties;

    public CfClient() {
    }

    public AmazonCloudFormation buildCfClient() {
        String accessKeyId = awsProperties.getCredential().getAccessKeyId();
        String secretKey = awsProperties.getCredential().getSecretKey();
        String region = awsProperties.getRegion();

        BasicAWSCredentials awsCredentials = new BasicAWSCredentials(accessKeyId, secretKey);

        return AmazonCloudFormationClientBuilder.standard()
                .withCredentials(new AWSStaticCredentialsProvider(awsCredentials))
                .withRegion(region)
                .build();
    }
}
