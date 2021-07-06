package com.sequenceiq.cloudbreak.cloud.aws.common;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

import com.amazonaws.auth.InstanceProfileCredentialsProvider;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonEc2Client;
import com.sequenceiq.cloudbreak.cloud.aws.common.mapper.SdkClientExceptionMapper;
import com.sequenceiq.cloudbreak.cloud.aws.common.view.AwsCredentialView;
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
    private CommonAwsClient awsClient;

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
        CloudContext context = CloudContext.Builder.builder()
                .withId(1L)
                .withName("context")
                .withCrn("crn")
                .withPlatform("AWS")
                .withVariant("AWS")
                .withLocation(Location.location(Region.region("country")))
                .withAccountId("account")
                .build();
        CloudCredential credential = new CloudCredential("id", "alma", parameters, false);
        AuthenticatedContext auth = underTest.authenticate(context, credential);
        assertTrue(auth.hasParameter(AmazonEc2Client.class.getName()),
                "Authenticated context does not have amazonClient after authentication");
        assertSame(amazonEC2Client, auth.getParameter(AmazonEc2Client.class));

        return auth;
    }

    @Configuration
    @Import({AwsAuthenticator.class,
            CommonAwsClient.class,
            AwsSessionCredentialClient.class,
            AwsDefaultZoneProvider.class,
            AwsEnvironmentVariableChecker.class,
            RetryService.class
    })
    static class Config {
    }
}
