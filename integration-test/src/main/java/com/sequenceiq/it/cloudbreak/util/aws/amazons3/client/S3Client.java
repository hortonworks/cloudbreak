package com.sequenceiq.it.cloudbreak.util.aws.amazons3.client;

import java.net.URI;
import java.net.URISyntaxException;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.sequenceiq.it.cloudbreak.cloud.v4.aws.AwsProperties;

@Service
public class S3Client {
    private static final Logger LOGGER = LoggerFactory.getLogger(S3Client.class);

    @Inject
    private AwsProperties awsProperties;

    public S3Client() {
    }

    public AmazonS3 buildS3Client() {
        String accessKeyId = awsProperties.getCredential().getAccessKeyId();
        String secretKey = awsProperties.getCredential().getSecretKey();
        String region = awsProperties.getRegion();

        BasicAWSCredentials awsCredentials = new BasicAWSCredentials(accessKeyId, secretKey);

        return AmazonS3ClientBuilder.standard()
                .withCredentials(new AWSStaticCredentialsProvider(awsCredentials))
                .withRegion(region)
                .build();
    }

    public String getBucketName() {
        try {
            URI uri = new URI(awsProperties.getCloudstorage().getBaseLocation());
            return uri.getHost();
        } catch (URISyntaxException e) {
            LOGGER.error("Amazon S3 Base Location could not been parsed, because of error!", e);
            return "cloudbreaktest";
        }
    }

    public String getEUWestS3Uri() {
        return String.join(".", "http://" + getBucketName(), "s3", awsProperties.getRegion(), "amazonaws.com");
    }
}
