package com.sequenceiq.it.cloudbreak.util.aws.amazons3.client;

import java.net.URI;
import java.net.URISyntaxException;

import javax.inject.Inject;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.sequenceiq.common.model.FileSystemType;
import com.sequenceiq.it.cloudbreak.cloud.v4.aws.AwsCloudProvider;
import com.sequenceiq.it.cloudbreak.cloud.v4.aws.AwsProperties;

@Service
public class S3Client {
    private static final Logger LOGGER = LoggerFactory.getLogger(S3Client.class);

    @Inject
    private AwsProperties awsProperties;

    @Inject
    private AwsCloudProvider awsCloudProvider;

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
        String baseLocation = awsProperties.getCloudStorage().getBaseLocation();
        try {
            URI uri = new URI(baseLocation);
            return uri.getHost();
        } catch (URISyntaxException e) {
            String[] parts = getObjects(baseLocation.replaceAll(FileSystemType.S3.getProtocol() + "://", ""));
            if (!StringUtils.isEmpty(baseLocation) && parts.length > 1) {
                return parts[0];
            } else {
                LOGGER.error("Amazon S3 Base Location could not been parsed, because of error!", e);
                return "cloudbreak-test";
            }
        }
    }

    public String getPath(String objectPath) {
        String[] parts = getObjects(objectPath);
        if (!StringUtils.isEmpty(objectPath) && parts.length > 3) {
            return StringUtils.join(ArrayUtils.removeAll(parts, 0, 1), "/");
        } else {
            LOGGER.debug("No path found in S3 location.");
            return "";
        }
    }

    public String getBaseLocationUri() {
        return String.join(".", "http://" + getBucketName(), "s3", awsProperties.getRegion(), "amazonaws.com");
    }

    private String[] getObjects(String splittable) {
        return splittable.split("/");
    }
}
