package com.sequenceiq.it.cloudbreak.util.amazonec2.client;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2ClientBuilder;
import com.sequenceiq.it.cloudbreak.cloud.v4.aws.AwsProperties;

@Service
public class EC2Client {
    private static final Logger LOGGER = LoggerFactory.getLogger(EC2Client.class);

    @Inject
    private AwsProperties awsProperties;

    public EC2Client() {
    }

    public AmazonEC2 buildEC2Client() {
        String accessKeyId = awsProperties.getCredential().getAccessKeyId();
        String secretKey = awsProperties.getCredential().getSecretKey();
        String region = awsProperties.getRegion();

        BasicAWSCredentials awsCredentials = new BasicAWSCredentials(accessKeyId, secretKey);

        return AmazonEC2ClientBuilder.standard()
                .withCredentials(new AWSStaticCredentialsProvider(awsCredentials))
                .withRegion(region)
                .build();
    }
}
