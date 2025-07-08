package com.sequenceiq.cloudbreak.cloud.aws.common.client;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.sequenceiq.cloudbreak.service.Retry;
import com.sequenceiq.cloudbreak.service.Retry.ActionFailedException;
import com.sequenceiq.cloudbreak.service.RetryService;

import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.CreateSecretRequest;
import software.amazon.awssdk.services.secretsmanager.model.DeleteSecretRequest;
import software.amazon.awssdk.services.secretsmanager.model.DescribeSecretRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetResourcePolicyRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.PutResourcePolicyRequest;
import software.amazon.awssdk.services.secretsmanager.model.UpdateSecretRequest;

class AmazonSecretsManagerClientTest {

    private SecretsManagerClient secretsManagerClient;

    private AmazonSecretsManagerClient underTest;

    @BeforeEach
    void setUp() {
        secretsManagerClient = mock(SecretsManagerClient.class);
        Retry retry = new RetryService();
        underTest = new AmazonSecretsManagerClient(secretsManagerClient, retry);
    }

    @ParameterizedTest(name = "When AWS execption code is {0} should throw ActionFailedException")
    @ValueSource(strings = {"InternalFailure", "InternalServiceError", "RequestExpired", "ServiceUnavailable"})
    void testGetSecretValueRetriableErrorCodes(String errorCode) {
        GetSecretValueRequest getSecretValueRequest = mock(GetSecretValueRequest.class);
        AwsServiceException exception = mock(AwsServiceException.class);
        AwsErrorDetails awsErrorDetails = mock(AwsErrorDetails.class);

        when(exception.awsErrorDetails()).thenReturn(awsErrorDetails);
        when(awsErrorDetails.errorCode()).thenReturn(errorCode);

        when(secretsManagerClient.getSecretValue(getSecretValueRequest)).thenThrow(exception);

        ActionFailedException result = assertThrows(ActionFailedException.class, () -> underTest.getSecretValue(getSecretValueRequest));
        assertInstanceOf(AwsServiceException.class, result.getCause());
    }

    @ParameterizedTest(name = "When AWS execption code is {0} should throw ActionFailedException")
    @ValueSource(strings = {"InternalFailure", "InternalServiceError", "RequestExpired", "ServiceUnavailable"})
    void testDescribeSecretRetriableErrorCodes(String errorCode) {
        DescribeSecretRequest describeSecretRequest = mock(DescribeSecretRequest.class);
        AwsServiceException exception = mock(AwsServiceException.class);
        AwsErrorDetails awsErrorDetails = mock(AwsErrorDetails.class);

        when(exception.awsErrorDetails()).thenReturn(awsErrorDetails);
        when(awsErrorDetails.errorCode()).thenReturn(errorCode);

        when(secretsManagerClient.describeSecret(describeSecretRequest)).thenThrow(exception);

        ActionFailedException result = assertThrows(ActionFailedException.class, () -> underTest.describeSecret(describeSecretRequest));
        assertInstanceOf(AwsServiceException.class, result.getCause());
    }

    @ParameterizedTest(name = "When AWS execption code is {0} should throw ActionFailedException")
    @ValueSource(strings = {"InternalFailure", "InternalServiceError", "RequestExpired", "ServiceUnavailable"})
    void testCreateSecretRetriableErrorCodes(String errorCode) {
        CreateSecretRequest createSecretRequest = mock(CreateSecretRequest.class);
        AwsServiceException exception = mock(AwsServiceException.class);
        AwsErrorDetails awsErrorDetails = mock(AwsErrorDetails.class);

        when(exception.awsErrorDetails()).thenReturn(awsErrorDetails);
        when(awsErrorDetails.errorCode()).thenReturn(errorCode);

        when(secretsManagerClient.createSecret(createSecretRequest)).thenThrow(exception);

        ActionFailedException result = assertThrows(ActionFailedException.class, () -> underTest.createSecret(createSecretRequest));
        assertInstanceOf(AwsServiceException.class, result.getCause());
    }

    @ParameterizedTest(name = "When AWS execption code is {0} should throw ActionFailedException")
    @ValueSource(strings = {"InternalFailure", "InternalServiceError", "RequestExpired", "ServiceUnavailable"})
    void testDeleteSecretRetriableErrorCodes(String errorCode) {
        DeleteSecretRequest deleteSecretRequest = mock(DeleteSecretRequest.class);
        AwsServiceException exception = mock(AwsServiceException.class);
        AwsErrorDetails awsErrorDetails = mock(AwsErrorDetails.class);

        when(exception.awsErrorDetails()).thenReturn(awsErrorDetails);
        when(awsErrorDetails.errorCode()).thenReturn(errorCode);

        when(secretsManagerClient.deleteSecret(deleteSecretRequest)).thenThrow(exception);

        ActionFailedException result = assertThrows(ActionFailedException.class, () -> underTest.deleteSecret(deleteSecretRequest));
        assertInstanceOf(AwsServiceException.class, result.getCause());
    }

    @ParameterizedTest(name = "When AWS execption code is {0} should throw ActionFailedException")
    @ValueSource(strings = {"InternalFailure", "InternalServiceError", "RequestExpired", "ServiceUnavailable"})
    void testUpdateSecretRetriableErrorCodes(String errorCode) {
        UpdateSecretRequest updateSecretRequest = mock(UpdateSecretRequest.class);
        AwsServiceException exception = mock(AwsServiceException.class);
        AwsErrorDetails awsErrorDetails = mock(AwsErrorDetails.class);

        when(exception.awsErrorDetails()).thenReturn(awsErrorDetails);
        when(awsErrorDetails.errorCode()).thenReturn(errorCode);

        when(secretsManagerClient.updateSecret(updateSecretRequest)).thenThrow(exception);

        ActionFailedException result = assertThrows(ActionFailedException.class, () -> underTest.updateSecret(updateSecretRequest));
        assertInstanceOf(AwsServiceException.class, result.getCause());
    }

    @ParameterizedTest(name = "When AWS execption code is {0} should throw ActionFailedException")
    @ValueSource(strings = {"InternalFailure", "InternalServiceError", "RequestExpired", "ServiceUnavailable"})
    void testPutResourcePolicyRetriableErrorCodes(String errorCode) {
        PutResourcePolicyRequest putResourcePolicyRequest = mock(PutResourcePolicyRequest.class);
        AwsServiceException exception = mock(AwsServiceException.class);
        AwsErrorDetails awsErrorDetails = mock(AwsErrorDetails.class);

        when(exception.awsErrorDetails()).thenReturn(awsErrorDetails);
        when(awsErrorDetails.errorCode()).thenReturn(errorCode);

        when(secretsManagerClient.putResourcePolicy(putResourcePolicyRequest)).thenThrow(exception);

        ActionFailedException result = assertThrows(ActionFailedException.class, () -> underTest.putResourcePolicy(putResourcePolicyRequest));
        assertInstanceOf(AwsServiceException.class, result.getCause());
    }

    @ParameterizedTest(name = "When AWS execption code is {0} should throw ActionFailedException")
    @ValueSource(strings = {"InternalFailure", "InternalServiceError", "RequestExpired", "ServiceUnavailable"})
    void testGetResourcePolicyRetriableErrorCodes(String errorCode) {
        GetResourcePolicyRequest getResourcePolicyRequest = mock(GetResourcePolicyRequest.class);
        AwsServiceException exception = mock(AwsServiceException.class);
        AwsErrorDetails awsErrorDetails = mock(AwsErrorDetails.class);

        when(exception.awsErrorDetails()).thenReturn(awsErrorDetails);
        when(awsErrorDetails.errorCode()).thenReturn(errorCode);

        when(secretsManagerClient.getResourcePolicy(getResourcePolicyRequest)).thenThrow(exception);

        ActionFailedException result = assertThrows(ActionFailedException.class, () -> underTest.getResourcePolicy(getResourcePolicyRequest));
        assertInstanceOf(AwsServiceException.class, result.getCause());
    }
}
