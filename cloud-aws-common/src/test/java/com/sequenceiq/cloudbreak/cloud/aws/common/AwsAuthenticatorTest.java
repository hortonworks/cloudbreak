package com.sequenceiq.cloudbreak.cloud.aws.common;

import static com.sequenceiq.cloudbreak.cloud.aws.common.view.AwsCredentialView.AWS;
import static com.sequenceiq.cloudbreak.cloud.aws.common.view.AwsCredentialView.DEFAULT_REGION_KEY;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonEc2Client;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AwsApacheClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.endpoint.AwsRegionEndpointProvider;
import com.sequenceiq.cloudbreak.cloud.aws.common.endpoint.AwsServiceEndpointProvider;
import com.sequenceiq.cloudbreak.cloud.aws.common.mapper.SdkClientExceptionMapper;
import com.sequenceiq.cloudbreak.cloud.aws.common.metrics.AwsMetricPublisher;
import com.sequenceiq.cloudbreak.cloud.aws.common.util.AwsPageCollector;
import com.sequenceiq.cloudbreak.cloud.aws.common.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.event.credential.CredentialVerificationException;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.cloud.model.Region;
import com.sequenceiq.cloudbreak.service.RetryService;

@ExtendWith(SpringExtension.class)
class AwsAuthenticatorTest {

    private static final String REGION = "country";

    @Inject
    private AwsAuthenticator underTest;

    @SpyBean
    private AwsApacheClient awsApacheClient;

    @SpyBean
    private CommonAwsClient awsClient;

    @MockBean
    private AwsEnvironmentVariableChecker awsEnvironmentVariableChecker;

    @Mock
    private AmazonEc2Client amazonEC2Client;

    @MockBean
    private SdkClientExceptionMapper sdkClientExceptionMapper;

    @MockBean
    private AwsPageCollector awsPageCollector;

    @MockBean
    private AwsMetricPublisher awsMetricPublisher;

    @BeforeEach
    void awsClientSetup() {
        doReturn(amazonEC2Client).when(awsClient).createEc2Client(any(AwsCredentialView.class));
        doReturn(amazonEC2Client).when(awsClient).createEc2Client(any(AwsCredentialView.class), anyString());
    }

    @Test
    void testAuthenticateWithAccessPairMissing() {
        doCallRealMethod().when(awsClient).createEc2Client(any(), anyString());
        assertThrows(CredentialVerificationException.class, () -> testAuthenticate(Map.of("accessKey", "ac")));
    }

    @Test
    void testAuthenticateWithAccessPairMissingSecret() {
        doCallRealMethod().when(awsClient).createEc2Client(any(), anyString());
        assertThrows(CredentialVerificationException.class, () -> testAuthenticate(Map.of("secretKey", "ac")));
    }

    @Test
    void testAuthenticateSucceedWithRole() {
        when(awsEnvironmentVariableChecker.isAwsAccessKeyAvailable(any(AwsCredentialView.class))).thenReturn(false);
        when(awsEnvironmentVariableChecker.isAwsSecretAccessKeyAvailable(any(AwsCredentialView.class))).thenReturn(false);
        testAuthenticate(Map.of(AWS, Map.of(DEFAULT_REGION_KEY, REGION, "roleBased", Map.of("roleArn", "role"))));
        verify(awsEnvironmentVariableChecker, times(2)).isAwsAccessKeyAvailable(any(AwsCredentialView.class));
        verify(awsEnvironmentVariableChecker, times(1)).isAwsSecretAccessKeyAvailable(any(AwsCredentialView.class));
    }

    @Test
    void testAuthenticateWithRoleWithoutAccessKeyEnv() {
        when(awsEnvironmentVariableChecker.isAwsAccessKeyAvailable(any(AwsCredentialView.class))).thenReturn(false);
        when(awsEnvironmentVariableChecker.isAwsSecretAccessKeyAvailable(any(AwsCredentialView.class))).thenReturn(true);
        assertThrows(CredentialVerificationException.class, () -> testAuthenticate(Map.of("roleArn", "role")));
    }

    @Test
    void testAuthenticateWithRoleWithoutSecretKeyEnv() {
        when(awsEnvironmentVariableChecker.isAwsAccessKeyAvailable(any(AwsCredentialView.class))).thenReturn(true);
        when(awsEnvironmentVariableChecker.isAwsSecretAccessKeyAvailable(any(AwsCredentialView.class))).thenReturn(false);
        assertThrows(CredentialVerificationException.class, () -> testAuthenticate(Map.of("roleArn", "role")));
    }

    private AuthenticatedContext testAuthenticate(Map<String, Object> parameters) {
        CloudContext context = CloudContext.Builder.builder()
                .withId(1L)
                .withName("context")
                .withCrn("crn")
                .withPlatform("AWS")
                .withVariant("AWS")
                .withLocation(Location.location(Region.region(REGION)))
                .withAccountId("account")
                .build();
        CloudCredential credential = new CloudCredential("id", "alma", parameters, "acc");
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
            RetryService.class,
            AwsRegionEndpointProvider.class,
            AwsServiceEndpointProvider.class
    })
    static class Config {
    }
}
