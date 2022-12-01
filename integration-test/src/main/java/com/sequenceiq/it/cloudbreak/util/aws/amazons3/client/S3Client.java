package com.sequenceiq.it.cloudbreak.util.aws.amazons3.client;

import static java.lang.String.format;

import java.net.URI;
import java.net.URISyntaxException;

import javax.inject.Inject;

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
import com.sequenceiq.it.cloudbreak.exception.TestFailException;

@Service
public class S3Client {
    private static final Logger LOGGER = LoggerFactory.getLogger(S3Client.class);

    @Inject
    private AwsProperties awsProperties;

    @Inject
    private AwsCloudProvider awsCloudProvider;

    public S3Client() {
    }

    public AwsProperties getAwsProperties() {
        return awsProperties;
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

    public String getDefaultBucketName() {
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

    public String getKeyPrefix(String baseLocation) {
        URI baseLocationUri = getBaseLocationUri(baseLocation);
        return StringUtils.removeStart(baseLocationUri.getPath(), "/");
    }

    public String getBucketName(String baseLocation) {
        URI baseLocationUri = getBaseLocationUri(baseLocation);
        return baseLocationUri.getHost();
    }

    public URI getDefaultBaseLocationUri() {
        String defaultBaseLocation = String.join(".", "http://" + getDefaultBucketName(), "s3", awsProperties.getRegion(), "amazonaws.com");
        try {
            return new URI(defaultBaseLocation);
        } catch (Exception e) {
            LOGGER.error("Default Amazon S3 base location path: '{}' is not a valid URI!", defaultBaseLocation);
            throw new TestFailException(format(" Default Amazon S3 base location path: '%s' is not a valid URI! ", defaultBaseLocation));
        }
    }

    public URI getBaseLocationUri(String baseLocation) {
        try {
            return new URI(baseLocation);
        } catch (Exception e) {
            LOGGER.error("Amazon S3 base location path: '{}' is not a valid URI!", baseLocation);
            throw new TestFailException(format(" Amazon S3 base location path: '%s' is not a valid URI! ", baseLocation));
        }
    }

    private String[] getObjects(String splittable) {
        return splittable.split("/");
    }
}
