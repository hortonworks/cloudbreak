package com.sequenceiq.cloudbreak.cloud.aws.common.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.SocketTimeoutException;

import org.aspectj.lang.Signature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.aws.common.util.AwsEncodedAuthorizationFailureMessageDecoder;
import com.sequenceiq.cloudbreak.cloud.aws.common.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.service.Retry;

import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.autoscaling.model.ScalingActivityInProgressException;

@ExtendWith(MockitoExtension.class)
class SdkClientExceptionMapperTest {

    private static final String REGION = "region";

    @InjectMocks
    private SdkClientExceptionMapper underTest;

    @Mock
    private AwsEncodedAuthorizationFailureMessageDecoder awsEncodedAuthorizationFailureMessageDecoder;

    @Mock
    private Signature signature;

    @Mock
    private AwsCredentialView ac;

    @BeforeEach
    void setup() {
        when(signature.getName()).thenReturn("methodName");
    }

    @Test
    void testMapWhenMethodNameAdded() {
        SdkException e = AwsServiceException.builder().message("message").awsErrorDetails(AwsErrorDetails.builder().build()).build();

        String message = "message (Service: null, Status Code: 0, Request ID: null)";
        when(awsEncodedAuthorizationFailureMessageDecoder.decodeAuthorizationFailureMessageIfNeeded(ac, REGION, message)).thenReturn(message);

        RuntimeException actual = underTest.map(ac, REGION, e, signature);

        assertEquals("Cannot execute method: methodName. " + message, actual.getMessage());
    }

    @Test
    public void testMapWhenMethodNameAddedAndNoErrorDetails() {
        SdkException e = AwsServiceException.builder().message("message").build();

        String message = "message";
        when(awsEncodedAuthorizationFailureMessageDecoder.decodeAuthorizationFailureMessageIfNeeded(ac, REGION, message)).thenReturn(message);

        RuntimeException actual = underTest.map(ac, REGION, e, signature);

        assertEquals("Cannot execute method: methodName. " + message, actual.getMessage());
    }

    @Test
    void testMapMessageEncodedAndMethodNameContained() {
        SdkClientException e = SdkClientException.create("message");

        when(awsEncodedAuthorizationFailureMessageDecoder.decodeAuthorizationFailureMessageIfNeeded(ac, REGION, "message")).thenReturn("encoded: methodName");

        RuntimeException actual = underTest.map(ac, REGION, e, signature);

        assertEquals(CloudConnectorException.class, actual.getClass());
        assertEquals("encoded: methodName", actual.getMessage());
    }

    @Test
    void testMapMessageRateExceededAndMethodNameContained() {
        String message = "Rate exceeded: methodName";
        SdkClientException e = SdkClientException.create(message);

        when(awsEncodedAuthorizationFailureMessageDecoder.decodeAuthorizationFailureMessageIfNeeded(ac, REGION, message)).thenReturn(message);

        RuntimeException actual = underTest.map(ac, REGION, e, signature);

        assertEquals(Retry.ActionFailedException.class, actual.getClass());
        assertEquals(message, actual.getMessage());
    }

    @Test
    void testMapMessageRateExceeded() {
        String message = "Rate exceeded";
        SdkClientException e = SdkClientException.create(message);

        when(awsEncodedAuthorizationFailureMessageDecoder.decodeAuthorizationFailureMessageIfNeeded(ac, REGION, message)).thenReturn(message);

        RuntimeException actual = underTest.map(ac, REGION, e, signature);

        assertEquals(Retry.ActionFailedException.class, actual.getClass());
        assertEquals("Cannot execute method: methodName. Rate exceeded", actual.getMessage());
    }

    @Test
    void testMapMessageRequestLimitExceededAndMethodNameContained() {
        String message = "Request limit exceeded: methodName";
        SdkClientException e = SdkClientException.create(message);

        when(awsEncodedAuthorizationFailureMessageDecoder.decodeAuthorizationFailureMessageIfNeeded(ac, REGION, message)).thenReturn(message);

        RuntimeException actual = underTest.map(ac, REGION, e, signature);

        assertEquals(Retry.ActionFailedException.class, actual.getClass());
        assertEquals(message, actual.getMessage());
    }

    @Test
    void testMapMessageRequestLimitExceeded() {
        String message = "Request limit exceeded";
        SdkClientException e = SdkClientException.create(message);

        when(awsEncodedAuthorizationFailureMessageDecoder.decodeAuthorizationFailureMessageIfNeeded(ac, REGION, message)).thenReturn(message);

        RuntimeException actual = underTest.map(ac, REGION, e, signature);

        assertEquals(Retry.ActionFailedException.class, actual.getClass());
        assertEquals("Cannot execute method: methodName. Request limit exceeded", actual.getMessage());
    }

    @Test
    void testMapScalingActivityInProgressExceptionToActionFailed() {
        String message = "Activity 123 is in progress. (Service: null, Status Code: 0, Request ID: null)";
        ScalingActivityInProgressException e = ScalingActivityInProgressException.builder()
                .message("Activity 123 is in progress.")
                .awsErrorDetails(AwsErrorDetails.builder().build())
                .build();

        when(awsEncodedAuthorizationFailureMessageDecoder.decodeAuthorizationFailureMessageIfNeeded(ac, REGION, message)).thenReturn(message);

        RuntimeException actual = underTest.map(ac, REGION, e, signature);

        assertEquals(Retry.ActionFailedException.class, actual.getClass());
        assertEquals(
                "Cannot execute method: methodName. Activity 123 is in progress. " +
                        "(Service: null, Status Code: 0, Request ID: null)", actual.getMessage());
    }

    @Test
    void testMapMessageUnableToUnmarshallResponse() {
        String message = "Unable to unmarshall response (Could not parse XML response.). Response Code: 200, Response Text: OK";
        SdkClientException e = SdkClientException.create(message);

        when(awsEncodedAuthorizationFailureMessageDecoder.decodeAuthorizationFailureMessageIfNeeded(ac, REGION, message)).thenReturn(message);

        RuntimeException actual = underTest.map(ac, REGION, e, signature);

        assertEquals(Retry.ActionFailedException.class, actual.getClass());
        assertEquals("Cannot execute method: methodName. Unable to unmarshall response (Could not parse XML response.). " +
                "Response Code: 200, Response Text: OK", actual.getMessage());
    }

    @Test
    void testMapMessageWhenSocketTimedOutBecauseOfReadTimeOut() {
        String message = "Unable to unmarshall response (Could not parse XML response.). Response Code: 200, Response Text: OK";
        SdkClientException e = SdkClientException.create("Could not parse XML response.", new SocketTimeoutException("Read timed out"));

        RuntimeException actual = underTest.map(ac, REGION, e, signature);

        assertEquals(Retry.ActionFailedException.class, actual.getClass());
        assertEquals("Cannot execute method: methodName. Failed to communicate with AWS provider: Read timed out", actual.getMessage());
        verify(awsEncodedAuthorizationFailureMessageDecoder, never()).decodeAuthorizationFailureMessageIfNeeded(any(), any(), any());
    }

    @Test
    void testMapMessageRandomError() {
        String message = "Random Aws Error Message";
        SdkClientException e = SdkClientException.create(message);

        when(awsEncodedAuthorizationFailureMessageDecoder.decodeAuthorizationFailureMessageIfNeeded(ac, REGION, message)).thenReturn(message);

        RuntimeException actual = underTest.map(ac, REGION, e, signature);

        assertEquals(SdkClientException.class, actual.getClass());
        assertEquals("Random Aws Error Message", actual.getMessage());
    }

}
