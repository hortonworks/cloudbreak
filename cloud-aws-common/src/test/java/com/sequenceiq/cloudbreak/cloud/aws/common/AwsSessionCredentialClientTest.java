package com.sequenceiq.cloudbreak.cloud.aws.common;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.cloud.aws.common.client.AwsApacheClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.view.AwsCredentialView;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sts.StsClient;

@ExtendWith(MockitoExtension.class)
class AwsSessionCredentialClientTest {

    @Mock
    private AwsDefaultZoneProvider awsDefaultZoneProvider;

    @Mock
    private AwsEnvironmentVariableChecker awsEnvironmentVariableChecker;

    @Mock
    private AwsCredentialView awsCredentialView;

    @InjectMocks
    @Spy
    private AwsSessionCredentialClient underTest;

    @Mock
    private AwsApacheClient awsApacheClient;

    @Test
    void testAwsSecurityTokenServiceClientWhenFipsEnabledAndGovCloudCredential() {
        String defaultRegion = setUpMocks(Boolean.TRUE, Boolean.TRUE);

        StsClient actual = underTest.awsSecurityTokenServiceClient(awsCredentialView);

        assertNotNull(actual);
        verify(underTest, times(0)).getEndpointConfiguration(defaultRegion);
    }

    @Test
    void testAwsSecurityTokenServiceClientWhenFipsEnabledAndNotAGovCloudCredential() {
        String defaultRegion = setUpMocks(Boolean.TRUE, Boolean.FALSE);

        StsClient actual = underTest.awsSecurityTokenServiceClient(awsCredentialView);

        assertNotNull(actual);
        verify(underTest, times(1)).getEndpointConfiguration(defaultRegion);
    }

    @Test
    void testAwsSecurityTokenServiceClientWhenFipsIsNotEnabledAndNotAGovCloudCredential() {
        String defaultRegion = setUpMocks(Boolean.FALSE, Boolean.FALSE);

        StsClient actual = underTest.awsSecurityTokenServiceClient(awsCredentialView);

        assertNotNull(actual);
        verify(underTest, times(1)).getEndpointConfiguration(defaultRegion);
    }

    @Test
    void testAwsSecurityTokenServiceClientWhenFipsIsNotEnabledAndGovCloudCredential() {
        String defaultRegion = setUpMocks(Boolean.FALSE, Boolean.TRUE);

        StsClient actual = underTest.awsSecurityTokenServiceClient(awsCredentialView);

        assertNotNull(actual);
        verify(underTest, times(1)).getEndpointConfiguration(defaultRegion);
    }

    private String setUpMocks(boolean fipsEnabled, boolean onGovCloud) {
        String defaultRegion = Region.EU_CENTRAL_1.toString();
        ReflectionTestUtils.setField(underTest, "fipsEnabled", fipsEnabled);
        when(awsEnvironmentVariableChecker.isAwsAccessKeyAvailable(awsCredentialView)).thenReturn(Boolean.FALSE);
        when(awsDefaultZoneProvider.getDefaultZone(awsCredentialView)).thenReturn(defaultRegion);
        lenient().when(awsCredentialView.isGovernmentCloudEnabled()).thenReturn(onGovCloud);
        return defaultRegion;
    }
}