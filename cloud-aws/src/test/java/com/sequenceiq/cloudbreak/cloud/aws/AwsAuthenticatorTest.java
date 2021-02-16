package com.sequenceiq.cloudbreak.cloud.aws;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Map;

import javax.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.amazonaws.AmazonClientException;
import com.amazonaws.auth.InstanceProfileCredentialsProvider;
import com.sequenceiq.cloudbreak.cloud.aws.client.AmazonEc2Client;
import com.sequenceiq.cloudbreak.cloud.aws.mapper.SdkClientExceptionMapper;
import com.sequenceiq.cloudbreak.cloud.aws.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.event.credential.CredentialVerificationException;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.cloud.model.Region;
import com.sequenceiq.cloudbreak.service.RetryService;

import io.opentracing.Tracer;

@ExtendWith(SpringExtension.class)
public class AwsAuthenticatorTest {

    @Inject
    private AwsAuthenticator underTest;

    @SpyBean
    private AwsClient awsClient;

    @MockBean
    private AwsEnvironmentVariableChecker awsEnvironmentVariableChecker;

    @Mock
    private AmazonEc2Client amazonEC2Client;

    @Mock
    private InstanceProfileCredentialsProvider instanceProfileCredentialsProvider;

    @MockBean
    private Tracer tracer;

    @MockBean
    private SdkClientExceptionMapper sdkClientExceptionMapper;

    @BeforeEach
    public void awsClientSetup() {
        doReturn(amazonEC2Client).when(awsClient).createEc2Client(any(AwsCredentialView.class));
        doReturn(amazonEC2Client).when(awsClient).createEc2Client(any(AwsCredentialView.class), anyString());
        doReturn(instanceProfileCredentialsProvider).when(awsClient).getInstanceProfileProvider();
    }

    @Test
    public void testAuthenticateWithAccessPairMissing() {
        doCallRealMethod().when(awsClient).createEc2Client(any(), anyString());
        Assertions.assertThrows(CredentialVerificationException.class, () -> testAuthenticate(Map.of("accessKey", "ac")));
    }

    @Test
    public void testAuthenticateWithAccessPairMissingSecret() {
        doCallRealMethod().when(awsClient).createEc2Client(any(), anyString());
        Assertions.assertThrows(CredentialVerificationException.class, () -> testAuthenticate(Map.of("secretKey", "ac")));
    }

    @Test
    public void testAuthenticateSucceedWithRole() {
        when(awsEnvironmentVariableChecker.isAwsAccessKeyAvailable(any(AwsCredentialView.class))).thenReturn(true);
        when(awsEnvironmentVariableChecker.isAwsSecretAccessKeyAvailable(any(AwsCredentialView.class))).thenReturn(true);
        testAuthenticate(Map.of("roleArn", "role"));
        verify(awsEnvironmentVariableChecker, times(1)).isAwsAccessKeyAvailable(any(AwsCredentialView.class));
        verify(awsEnvironmentVariableChecker, times(1)).isAwsSecretAccessKeyAvailable(any(AwsCredentialView.class));
    }

    @Test
    public void testAuthenticateSucceedWithRoleOnEC2Machine() {
        when(awsEnvironmentVariableChecker.isAwsAccessKeyAvailable(any(AwsCredentialView.class))).thenReturn(false);
        when(awsEnvironmentVariableChecker.isAwsSecretAccessKeyAvailable(any(AwsCredentialView.class))).thenReturn(false);
        testAuthenticate(Map.of("roleArn", "role"));
        verify(awsEnvironmentVariableChecker, times(1)).isAwsAccessKeyAvailable(any(AwsCredentialView.class));
        verify(awsEnvironmentVariableChecker, times(1)).isAwsSecretAccessKeyAvailable(any(AwsCredentialView.class));
        verify(instanceProfileCredentialsProvider, times(1)).getCredentials();
    }

    @Test
    public void testAuthenticateSucceedWithRoleOnEC2MachineProviderFailure() {
        when(awsEnvironmentVariableChecker.isAwsAccessKeyAvailable(any(AwsCredentialView.class))).thenReturn(false);
        when(awsEnvironmentVariableChecker.isAwsSecretAccessKeyAvailable(any(AwsCredentialView.class))).thenReturn(false);
        when(awsClient.getInstanceProfileProvider()).thenAnswer(invocation -> {
            throw new IOException();
        });

        Assertions.assertThrows(CredentialVerificationException.class, () -> testAuthenticate(Map.of("roleArn", "role")));
    }

    @Test
    public void testAuthenticateSucceedWithRoleOnEC2MachineProviderGetCredentialFailure() {
        when(awsEnvironmentVariableChecker.isAwsAccessKeyAvailable(any(AwsCredentialView.class))).thenReturn(false);
        when(awsEnvironmentVariableChecker.isAwsSecretAccessKeyAvailable(any(AwsCredentialView.class))).thenReturn(false);
        when(instanceProfileCredentialsProvider.getCredentials()).thenThrow(AmazonClientException.class);

        Assertions.assertThrows(CredentialVerificationException.class, () -> testAuthenticate(Map.of("roleArn", "role")));
    }

    @Test
    public void testAuthenticateWithRoleWithoutAccessKeyEnv() {
        when(awsEnvironmentVariableChecker.isAwsAccessKeyAvailable(any(AwsCredentialView.class))).thenReturn(false);
        when(awsEnvironmentVariableChecker.isAwsSecretAccessKeyAvailable(any(AwsCredentialView.class))).thenReturn(true);
        Assertions.assertThrows(CredentialVerificationException.class, () -> testAuthenticate(Map.of("roleArn", "role")));
    }

    @Test
    public void testAuthenticateWithRoleWithoutSecretKeyEnv() {
        when(awsEnvironmentVariableChecker.isAwsAccessKeyAvailable(any(AwsCredentialView.class))).thenReturn(true);
        when(awsEnvironmentVariableChecker.isAwsSecretAccessKeyAvailable(any(AwsCredentialView.class))).thenReturn(false);
        Assertions.assertThrows(CredentialVerificationException.class, () -> testAuthenticate(Map.of("roleArn", "role")));
    }

    private AuthenticatedContext testAuthenticate(Map<String, Object> parameters) {
        CloudContext context = new CloudContext(1L, "context", "crn", "AWS", "AWS", Location.location(Region.region("country")), "user", "account");
        CloudCredential credential = new CloudCredential("id", "alma", parameters, false);
        AuthenticatedContext auth = underTest.authenticate(context, credential);
        assertTrue(auth.hasParameter(AmazonEc2Client.class.getName()),
                "Authenticated context does not have amazonClient after authentication");
        assertSame(amazonEC2Client, auth.getParameter(AmazonEc2Client.class));

        return auth;
    }

    @Configuration
    @Import({AwsAuthenticator.class,
            AwsClient.class,
            AwsSessionCredentialClient.class,
            AwsDefaultZoneProvider.class,
            AwsEnvironmentVariableChecker.class,
            RetryService.class
    })
    static class Config {
    }
}
