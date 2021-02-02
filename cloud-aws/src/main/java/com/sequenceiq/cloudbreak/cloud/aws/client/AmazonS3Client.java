package com.sequenceiq.cloudbreak.cloud.aws.client;

import com.amazonaws.services.s3.AmazonS3;

public class AmazonS3Client extends AmazonClient {

    private final AmazonS3 client;

    public AmazonS3Client(AmazonS3 client) {
        this.client = client;
    }

    public String getBucketLocation(String bucketName) {
        return client.getBucketLocation(bucketName);
    }
}
