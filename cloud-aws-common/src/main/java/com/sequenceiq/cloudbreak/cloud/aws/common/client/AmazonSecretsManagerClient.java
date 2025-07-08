package com.sequenceiq.cloudbreak.cloud.aws.common.client;

import static com.sequenceiq.cloudbreak.cloud.aws.common.AwsSdkErrorCodes.INTERNAL_FAILURE;
import static com.sequenceiq.cloudbreak.cloud.aws.common.AwsSdkErrorCodes.INTERNAL_SERVICE_ERROR;
import static com.sequenceiq.cloudbreak.cloud.aws.common.AwsSdkErrorCodes.REQUEST_EXPIRED;
import static com.sequenceiq.cloudbreak.cloud.aws.common.AwsSdkErrorCodes.SERVICE_UNAVAILABLE;

import java.util.Set;

import com.sequenceiq.cloudbreak.service.Retry;
import com.sequenceiq.cloudbreak.service.Retry.ActionFailedException;

import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.CreateSecretRequest;
import software.amazon.awssdk.services.secretsmanager.model.CreateSecretResponse;
import software.amazon.awssdk.services.secretsmanager.model.DeleteSecretRequest;
import software.amazon.awssdk.services.secretsmanager.model.DeleteSecretResponse;
import software.amazon.awssdk.services.secretsmanager.model.DescribeSecretRequest;
import software.amazon.awssdk.services.secretsmanager.model.DescribeSecretResponse;
import software.amazon.awssdk.services.secretsmanager.model.GetResourcePolicyRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetResourcePolicyResponse;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;
import software.amazon.awssdk.services.secretsmanager.model.PutResourcePolicyRequest;
import software.amazon.awssdk.services.secretsmanager.model.PutResourcePolicyResponse;
import software.amazon.awssdk.services.secretsmanager.model.UpdateSecretRequest;
import software.amazon.awssdk.services.secretsmanager.model.UpdateSecretResponse;

public class AmazonSecretsManagerClient extends AmazonClient {
    private static final Set<String> RETRIABLE_ERRORS = Set.of(INTERNAL_FAILURE, INTERNAL_SERVICE_ERROR, REQUEST_EXPIRED, SERVICE_UNAVAILABLE);

    private final SecretsManagerClient secretsManagerClient;

    private final Retry retry;

    public AmazonSecretsManagerClient(SecretsManagerClient secretsManagerClient, Retry retry) {
        this.secretsManagerClient = secretsManagerClient;
        this.retry = retry;
    }

    public GetSecretValueResponse getSecretValue(GetSecretValueRequest getSecretValueRequest) {
        return retry.testWith2SecDelayMax15Times(() -> {
            try {
                return secretsManagerClient.getSecretValue(getSecretValueRequest);
            } catch (AwsServiceException ex) {
                throw createActionFailedExceptionIfRetriableError(ex);
            }
        });
    }

    public DescribeSecretResponse describeSecret(DescribeSecretRequest describeSecretRequest) {
        return retry.testWith2SecDelayMax15Times(() -> {
            try {
                return secretsManagerClient.describeSecret(describeSecretRequest);
            } catch (AwsServiceException ex) {
                throw createActionFailedExceptionIfRetriableError(ex);
            }
        });
    }

    public CreateSecretResponse createSecret(CreateSecretRequest createSecretRequest) {
        return retry.testWith2SecDelayMax15Times(() -> {
            try {
                return secretsManagerClient.createSecret(createSecretRequest);
            } catch (AwsServiceException ex) {
                throw createActionFailedExceptionIfRetriableError(ex);
            }
        });
    }

    public DeleteSecretResponse deleteSecret(DeleteSecretRequest deleteSecretRequest) {
        return retry.testWith2SecDelayMax15Times(() -> {
            try {
                return secretsManagerClient.deleteSecret(deleteSecretRequest);
            } catch (AwsServiceException ex) {
                throw createActionFailedExceptionIfRetriableError(ex);
            }
        });
    }

    public UpdateSecretResponse updateSecret(UpdateSecretRequest updateSecretRequest) {
        return retry.testWith2SecDelayMax15Times(() -> {
            try {
                return secretsManagerClient.updateSecret(updateSecretRequest);
            } catch (AwsServiceException ex) {
                throw createActionFailedExceptionIfRetriableError(ex);
            }
        });
    }

    public PutResourcePolicyResponse putResourcePolicy(PutResourcePolicyRequest putResourcePolicyRequest) {
        return retry.testWith2SecDelayMax15Times(() -> {
            try {
                return secretsManagerClient.putResourcePolicy(putResourcePolicyRequest);
            } catch (AwsServiceException ex) {
                throw createActionFailedExceptionIfRetriableError(ex);
            }
        });
    }

    public GetResourcePolicyResponse getResourcePolicy(GetResourcePolicyRequest getResourcePolicyRequest) {
        return retry.testWith2SecDelayMax15Times(() -> {
            try {
                return secretsManagerClient.getResourcePolicy(getResourcePolicyRequest);
            } catch (AwsServiceException ex) {
                throw createActionFailedExceptionIfRetriableError(ex);
            }
        });
    }

    private RuntimeException createActionFailedExceptionIfRetriableError(AwsServiceException ex) {
        if (ex.awsErrorDetails() != null) {
            String errorCode = ex.awsErrorDetails().errorCode();
            if (RETRIABLE_ERRORS.contains(errorCode)) {
                return new ActionFailedException(ex);
            }
        }
        return ex;
    }
}
