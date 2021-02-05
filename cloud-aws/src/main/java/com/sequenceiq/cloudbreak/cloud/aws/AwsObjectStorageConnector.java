package com.sequenceiq.cloudbreak.cloud.aws;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.cloud.ObjectStorageConnector;
import com.sequenceiq.cloudbreak.cloud.aws.client.AmazonIdentityManagementClient;
import com.sequenceiq.cloudbreak.cloud.aws.client.AmazonS3Client;
import com.sequenceiq.cloudbreak.cloud.aws.validator.AwsIDBrokerObjectStorageValidator;
import com.sequenceiq.cloudbreak.cloud.aws.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.SpiFileSystem;
import com.sequenceiq.cloudbreak.cloud.model.Variant;
import com.sequenceiq.cloudbreak.cloud.model.base.ResponseStatus;
import com.sequenceiq.cloudbreak.cloud.model.objectstorage.ObjectStorageMetadataRequest;
import com.sequenceiq.cloudbreak.cloud.model.objectstorage.ObjectStorageMetadataResponse;
import com.sequenceiq.cloudbreak.cloud.model.objectstorage.ObjectStorageValidateRequest;
import com.sequenceiq.cloudbreak.cloud.model.objectstorage.ObjectStorageValidateResponse;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.cloudbreak.validation.ValidationResult.ValidationResultBuilder;

@Service
public class AwsObjectStorageConnector implements ObjectStorageConnector {

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsObjectStorageConnector.class);

    private static final int ACCESS_DENIED_ERROR_CODE = 403;

    @Inject
    private AwsClient awsClient;

    @Inject
    private AwsIDBrokerObjectStorageValidator awsIDBrokerObjectStorageValidator;

    @Inject
    private EntitlementService entitlementService;

    @Override
    public ObjectStorageMetadataResponse getObjectStorageMetadata(ObjectStorageMetadataRequest request) {
        AwsCredentialView awsCredentialView = new AwsCredentialView(request.getCredential());
        try {
            AmazonS3Client s3Client = awsClient.createS3Client(awsCredentialView);
            String bucketLocation = fixBucketLocation(s3Client.getBucketLocation(request.getObjectStoragePath()));
            return ObjectStorageMetadataResponse.builder()
                    .withRegion(bucketLocation)
                    .withStatus(ResponseStatus.OK)
                    .build();
        } catch (AmazonS3Exception e) {
            // HACK let's assume that if the user gets back 403 Access Denied it is because s/he does not have the s3:GetBucketLocation permission.
            // It is also true though that if the bucket indeed exists but it is in another account or otherwise denied from the requesting user,
            // the same error code will be returned. However, this hack is mainly for QAAS.
            if (e.getStatusCode() != ACCESS_DENIED_ERROR_CODE) {
                throw new CloudConnectorException(String.format("Cannot get object storage location for %s. "
                        + "Provider error message: %s", request.getObjectStoragePath(), e.getErrorMessage()), e);
            }
            return ObjectStorageMetadataResponse.builder()
                    .withStatus(ResponseStatus.ACCESS_DENIED)
                    .build();
        }
    }

    @Override
    public ObjectStorageValidateResponse validateObjectStorage(ObjectStorageValidateRequest request) {
        String accountId = Crn.safeFromString(request.getCredential().getId()).getAccountId();
        if (!entitlementService.awsCloudStorageValidationEnabled(accountId)) {
            LOGGER.info("Aws Cloud storage validation entitlement is missing, not validating cloudStorageRequest: {}",
                    JsonUtil.writeValueAsStringSilent(request));
            return ObjectStorageValidateResponse.builder().withStatus(ResponseStatus.OK).build();
        }
        AwsCredentialView awsCredentialView = new AwsCredentialView(request.getCredential());
        AmazonIdentityManagementClient iam = awsClient.createAmazonIdentityManagement(awsCredentialView);
        SpiFileSystem spiFileSystem = request.getSpiFileSystem();
        ValidationResultBuilder resultBuilder = new ValidationResultBuilder();
        resultBuilder.prefix("Cloud Storage validation failed");
        ValidationResult validationResult = awsIDBrokerObjectStorageValidator.validateObjectStorage(
                iam, spiFileSystem, request.getLogsLocationBase(), resultBuilder);
        ObjectStorageValidateResponse response;
        if (validationResult.hasError()) {
            response = ObjectStorageValidateResponse.builder()
                    .withStatus(ResponseStatus.ERROR)
                    .withError(validationResult.getFormattedErrors())
                    .build();
        } else {
            response = ObjectStorageValidateResponse.builder()
                    .withStatus(ResponseStatus.OK)
                    .build();
        }
        return response;
    }

    /**
     * AWS SDK 1.xx returns "US" as the location for the buckets that are in region 'us-east-1'. It is an SDK bug.
     * AWS SDK 2.xx returns "" (empty string) for the same.
     * This function fixes these anomalies.
     *
     * @param bucketLocation bucket location returned by AWS SDK
     * @return fixed bucket location
     */
    private String fixBucketLocation(String bucketLocation) {
        if (bucketLocation.isEmpty() || "US".equals(bucketLocation)) {
            return "us-east-1";
        }
        return bucketLocation;
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
