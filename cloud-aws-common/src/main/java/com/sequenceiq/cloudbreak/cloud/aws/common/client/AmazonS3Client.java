package com.sequenceiq.cloudbreak.cloud.aws.common.client;

import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetBucketLocationRequest;
import software.amazon.awssdk.services.s3.model.GetBucketLocationResponse;

public class AmazonS3Client extends AmazonClient {

    private final S3Client client;

    public AmazonS3Client(S3Client client) {
        this.client = client;
    }

    public String getBucketLocation(String bucketName) {
        GetBucketLocationRequest request = GetBucketLocationRequest.builder().bucket(bucketName).build();
        GetBucketLocationResponse response = client.getBucketLocation(request);
        return response.locationConstraintAsString();
    }
}
