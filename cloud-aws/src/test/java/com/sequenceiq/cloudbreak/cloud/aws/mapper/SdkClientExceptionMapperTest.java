package com.sequenceiq.cloudbreak.cloud.aws.mapper;

import static org.mockito.Mockito.when;

import org.aspectj.lang.Signature;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.SdkClientException;
import com.sequenceiq.cloudbreak.cloud.aws.util.AwsEncodedAuthorizationFailureMessageDecoder;
import com.sequenceiq.cloudbreak.cloud.aws.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.service.Retry;

@ExtendWith(MockitoExtension.class)
public class SdkClientExceptionMapperTest {

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
    private void setup() {
        when(signature.getName()).thenReturn("methodName");
    }

    @Test
    public void testMapWhenMethodNameAdded() {
        SdkClientException e = new AmazonServiceException("message");

        String message = "message (Service: null; Status Code: 0; Error Code: null; Request ID: null; Proxy: null)";
        when(awsEncodedAuthorizationFailureMessageDecoder.decodeAuthorizationFailureMessageIfNeeded(ac, REGION, message)).thenReturn(message);

        RuntimeException actual = underTest.map(ac, REGION, e, signature);

        Assertions.assertEquals("Cannot execute method: methodName. " + message, actual.getMessage());
    }

    @Test
    public void testMapMessageEncodedAndMethodNameContained() {
        SdkClientException e = new SdkClientException("message");

        when(awsEncodedAuthorizationFailureMessageDecoder.decodeAuthorizationFailureMessageIfNeeded(ac, REGION, "message")).thenReturn("encoded: methodName");

        RuntimeException actual = underTest.map(ac, REGION, e, signature);

        Assertions.assertEquals(CloudConnectorException.class, actual.getClass());
        Assertions.assertEquals("encoded: methodName", actual.getMessage());
    }

    @Test
    public void testMapMessageRateExceededAndMethodNameContained() {
        String message = "Rate exceeded: methodName";
        SdkClientException e = new SdkClientException(message);

        when(awsEncodedAuthorizationFailureMessageDecoder.decodeAuthorizationFailureMessageIfNeeded(ac, REGION, message)).thenReturn(message);

        RuntimeException actual = underTest.map(ac, REGION, e, signature);

        Assertions.assertEquals(Retry.ActionFailedException.class, actual.getClass());
        Assertions.assertEquals(message, actual.getMessage());
    }

    @Test
    public void testMapMessageRateExceeded() {
        String message = "Rate exceeded";
        SdkClientException e = new SdkClientException(message);

        when(awsEncodedAuthorizationFailureMessageDecoder.decodeAuthorizationFailureMessageIfNeeded(ac, REGION, message)).thenReturn(message);

        RuntimeException actual = underTest.map(ac, REGION, e, signature);

        Assertions.assertEquals(Retry.ActionFailedException.class, actual.getClass());
        Assertions.assertEquals("Cannot execute method: methodName. Rate exceeded", actual.getMessage());
    }

    @Test
    public void testMapMessageRequestLimitExceededAndMethodNameContained() {
        String message = "Request limit exceeded: methodName";
        SdkClientException e = new SdkClientException(message);

        when(awsEncodedAuthorizationFailureMessageDecoder.decodeAuthorizationFailureMessageIfNeeded(ac, REGION, message)).thenReturn(message);

        RuntimeException actual = underTest.map(ac, REGION, e, signature);

        Assertions.assertEquals(Retry.ActionFailedException.class, actual.getClass());
        Assertions.assertEquals(message, actual.getMessage());
    }

    @Test
    public void testMapMessageRequestLimitExceeded() {
        String message = "Request limit exceeded";
        SdkClientException e = new SdkClientException(message);

        when(awsEncodedAuthorizationFailureMessageDecoder.decodeAuthorizationFailureMessageIfNeeded(ac, REGION, message)).thenReturn(message);

        RuntimeException actual = underTest.map(ac, REGION, e, signature);

        Assertions.assertEquals(Retry.ActionFailedException.class, actual.getClass());
        Assertions.assertEquals("Cannot execute method: methodName. Request limit exceeded", actual.getMessage());
    }
}
