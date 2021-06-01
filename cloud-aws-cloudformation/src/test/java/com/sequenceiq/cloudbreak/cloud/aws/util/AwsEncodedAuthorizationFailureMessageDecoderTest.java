package com.sequenceiq.cloudbreak.cloud.aws.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.amazonaws.services.securitytoken.model.AWSSecurityTokenServiceException;
import com.amazonaws.services.securitytoken.model.DecodeAuthorizationMessageRequest;
import com.amazonaws.services.securitytoken.model.DecodeAuthorizationMessageResult;
import com.github.jknack.handlebars.internal.Files;
import com.sequenceiq.cloudbreak.cloud.aws.AwsClient;
import com.sequenceiq.cloudbreak.cloud.aws.client.AmazonSecurityTokenServiceClient;
import com.sequenceiq.cloudbreak.cloud.aws.view.AwsCredentialView;

@ExtendWith(MockitoExtension.class)
class AwsEncodedAuthorizationFailureMessageDecoderTest {

    private static final String ENCODED_AUTHORIZATION_FAILURE_MESSAGE =
            "API: ec2:CreateSecurityGroup You are not authorized to perform this operation. Encoded authorization failure message: encoded-message";

    private static final String REGION = "us-west-1";

    private static String decodedMessage;

    @Mock
    private AwsClient awsClient;

    @Mock
    private AmazonSecurityTokenServiceClient awsSecurityTokenService;

    @Mock
    private AwsCredentialView awsCredentialView;

    @InjectMocks
    private AwsEncodedAuthorizationFailureMessageDecoder underTest;

    @Captor
    private ArgumentCaptor<DecodeAuthorizationMessageRequest> requestCaptor;

    @BeforeAll
    static void init() throws IOException {
        decodedMessage = Files.read(new File("src/test/resources/json/aws-decoded-authorization-error.json"), Charset.defaultCharset());
    }

    @BeforeEach
    void setUp() {
        lenient().when(awsClient.createSecurityTokenService(any(), eq(REGION)))
                .thenReturn(awsSecurityTokenService);
        lenient().when(awsSecurityTokenService.decodeAuthorizationMessage(any()))
                .thenReturn(new DecodeAuthorizationMessageResult().withDecodedMessage(decodedMessage));
    }

    @Test
    void shouldReturnUnmodifiedMessageWhenMessageIsNotEncoded() {
        String message = "Resource never entered the desired state as it failed.";

        String result = underTest.decodeAuthorizationFailureMessageIfNeeded(awsCredentialView, REGION, message);

        assertThat(result).isEqualTo(message);
        verifyNoInteractions(awsClient);
        verifyNoInteractions(awsSecurityTokenService);
    }

    @Test
    void shouldDecodeEncodedMessage() {
        String result = underTest.decodeAuthorizationFailureMessageIfNeeded(awsCredentialView, REGION, ENCODED_AUTHORIZATION_FAILURE_MESSAGE);

        assertThat(result).isEqualTo("Your AWS credential is not authorized to perform action ec2:CreateSecurityGroup on resource " +
                "arn:aws:ec2:eu-central-1:123456789101:vpc/vpc-id. Please contact your system administrator to update your AWS policy.");
        verify(awsClient).createSecurityTokenService(awsCredentialView, REGION);
        verify(awsSecurityTokenService).decodeAuthorizationMessage(requestCaptor.capture());
        DecodeAuthorizationMessageRequest request = requestCaptor.getValue();
        assertThat(request.getEncodedMessage()).isEqualTo("encoded-message");
    }

    @Test
    void shouldReturnMessageWithWarningWhenStsAccessIsDenied() {
        AWSSecurityTokenServiceException exception = new AWSSecurityTokenServiceException("AccessDenied");
        exception.setErrorCode("AccessDenied");
        when(awsSecurityTokenService.decodeAuthorizationMessage(any()))
                .thenThrow(exception);

        String result = underTest.decodeAuthorizationFailureMessageIfNeeded(awsCredentialView, REGION, ENCODED_AUTHORIZATION_FAILURE_MESSAGE);
        assertThat(result).isEqualTo("API: ec2:CreateSecurityGroup Your credential is not authorized to perform the requested action on AWS side. " +
                "Please contact your system administrator to update your AWS policy with the sts:DecodeAuthorizationMessage " +
                "permission to get more details next time.");
        verify(awsClient).createSecurityTokenService(awsCredentialView, REGION);
        verify(awsSecurityTokenService).decodeAuthorizationMessage(any());
    }

    @Test
    void shouldReturnDefaultMessageWhenStsThrowsOtherException() {
        AWSSecurityTokenServiceException exception = new AWSSecurityTokenServiceException("SomethingWentWrong");
        when(awsSecurityTokenService.decodeAuthorizationMessage(any()))
                .thenThrow(exception);

        String result = underTest.decodeAuthorizationFailureMessageIfNeeded(awsCredentialView, REGION, ENCODED_AUTHORIZATION_FAILURE_MESSAGE);
        assertThat(result).isEqualTo("API: ec2:CreateSecurityGroup Your credential is not authorized to perform the requested action on AWS side.");
        verify(awsClient).createSecurityTokenService(awsCredentialView, REGION);
        verify(awsSecurityTokenService).decodeAuthorizationMessage(any());
    }

    @Test
    void shouldReturnDefaultMessageWhenNonStsExceptionIsThrown() {
        Exception exception = new RuntimeException("SomethingWentWrong");
        when(awsSecurityTokenService.decodeAuthorizationMessage(any()))
                .thenThrow(exception);

        String result = underTest.decodeAuthorizationFailureMessageIfNeeded(awsCredentialView, REGION, ENCODED_AUTHORIZATION_FAILURE_MESSAGE);
        assertThat(result).isEqualTo("API: ec2:CreateSecurityGroup Your credential is not authorized to perform the requested action on AWS side.");
        verify(awsClient).createSecurityTokenService(awsCredentialView, REGION);
        verify(awsSecurityTokenService).decodeAuthorizationMessage(any());
    }

}
