package com.sequenceiq.cloudbreak.cloud.aws;

import org.springframework.stereotype.Service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.sequenceiq.cloudbreak.cloud.ObjectStorageConnector;
import com.sequenceiq.cloudbreak.cloud.aws.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.Variant;
import com.sequenceiq.cloudbreak.cloud.model.objectstorage.ObjectStorageMetadataRequest;
import com.sequenceiq.cloudbreak.cloud.model.objectstorage.ObjectStorageMetadataResponse;

@Service
public class AwsObjectStorageConnector implements ObjectStorageConnector {

    private final AwsClient awsClient;

    public AwsObjectStorageConnector(AwsClient awsClient) {
        this.awsClient = awsClient;
    }

    @Override
    public ObjectStorageMetadataResponse getObjectStorageMetadata(ObjectStorageMetadataRequest request) {
        AwsCredentialView awsCredentialView = new AwsCredentialView(request.getCredential());
        try {
            AmazonS3 s3Client = awsClient.createS3Client(awsCredentialView);
            String bucketLocation = s3Client.getBucketLocation(request.getObjectStoragePath());
            return ObjectStorageMetadataResponse.builder()
                    .withRegion(bucketLocation)
                    .build();
        } catch (AmazonS3Exception e) {
            throw new CloudConnectorException(String.format("Cannot get object storage location for %s. "
                    + "Provider error message: %s", request.getObjectStoragePath(), e.getErrorMessage()), e);

        }
    }

    @Override
    public Platform platform() {
        return AwsConstants.AWS_PLATFORM;
    }

    @Override
    public Variant variant() {
        return AwsConstants.AWS_VARIANT;
    }
}
