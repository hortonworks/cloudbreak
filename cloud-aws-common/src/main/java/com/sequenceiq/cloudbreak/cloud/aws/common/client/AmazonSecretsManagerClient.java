package com.sequenceiq.cloudbreak.cloud.aws.common.client;

import com.sequenceiq.cloudbreak.service.Retry;

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

    private final SecretsManagerClient secretsManagerClient;

    private final Retry retry;

    public AmazonSecretsManagerClient(SecretsManagerClient secretsManagerClient, Retry retry) {
        this.secretsManagerClient = secretsManagerClient;
        this.retry = retry;
    }

    public GetSecretValueResponse getSecretValue(GetSecretValueRequest getSecretValueRequest) {
        return retry.testWith2SecDelayMax15Times(() -> secretsManagerClient.getSecretValue(getSecretValueRequest));
    }

    public DescribeSecretResponse describeSecret(DescribeSecretRequest describeSecretRequest) {
        return retry.testWith2SecDelayMax15Times(() -> secretsManagerClient.describeSecret(describeSecretRequest));
    }

    public CreateSecretResponse createSecret(CreateSecretRequest createSecretRequest) {
        return retry.testWith2SecDelayMax15Times(() -> secretsManagerClient.createSecret(createSecretRequest));
    }

    public DeleteSecretResponse deleteSecret(DeleteSecretRequest deleteSecretRequest) {
        return retry.testWith2SecDelayMax15Times(() -> secretsManagerClient.deleteSecret(deleteSecretRequest));
    }

    public UpdateSecretResponse updateSecret(UpdateSecretRequest updateSecretRequest) {
        return retry.testWith2SecDelayMax15Times(() -> secretsManagerClient.updateSecret(updateSecretRequest));
    }

    public PutResourcePolicyResponse putResourcePolicy(PutResourcePolicyRequest putResourcePolicyRequest) {
        return retry.testWith2SecDelayMax15Times(() -> secretsManagerClient.putResourcePolicy(putResourcePolicyRequest));
    }

    public GetResourcePolicyResponse getResourcePolicy(GetResourcePolicyRequest getResourcePolicyRequest) {
        return retry.testWith2SecDelayMax15Times(() -> secretsManagerClient.getResourcePolicy(getResourcePolicyRequest));
    }
}
