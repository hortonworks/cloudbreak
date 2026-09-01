package com.sequenceiq.cloudbreak.cloud.aws.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.cloud.aws.common.client.AwsApacheClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.view.AwsCredentialView;

import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.auth.StsAssumeRoleCredentialsProvider;
import software.amazon.awssdk.services.sts.model.AssumeRoleRequest;
import software.amazon.awssdk.services.sts.model.AssumeRoleResponse;
import software.amazon.awssdk.services.sts.model.Credentials;

@ExtendWith(MockitoExtension.class)
class AwsSessionCredentialClientTest {

    private static final String ROLE_ARN = "arn:aws:iam::123456789012:role/customer-role";

    private static final String DELEGATOR_ROLE_ARN = "arn:aws:iam::392479084068:role/mow-cloudbreak-delegator-role";

    private static final String EXTERNAL_ID = "external-id";

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

    @Test
    void retrieveSessionCredentialsSucceedsWithSaRoleWhenDelegatorConfigured() {
        setUpDelegatorConfig(DELEGATOR_ROLE_ARN);
        StsClient mockStsClient = mockSuccessfulStsClient();
        doReturn(mockStsClient).when(underTest).awsSecurityTokenServiceClient(awsCredentialView);
        setUpCredentialView();

        AwsSessionCredentials result = underTest.retrieveSessionCredentials(awsCredentialView);

        assertNotNull(result);
        assertEquals("accessKey", result.accessKeyId());
        verify(mockStsClient, times(1)).assumeRole(any(AssumeRoleRequest.class));
    }

    @Test
    void retrieveSessionCredentialsFallsBackToDelegatorWhenSaFails() {
        setUpDelegatorConfig(DELEGATOR_ROLE_ARN);
        StsClient failingStsClient = mock(StsClient.class);
        when(failingStsClient.assumeRole(any(AssumeRoleRequest.class)))
                .thenThrow(SdkException.builder().message("Access Denied").build());
        doReturn(failingStsClient).when(underTest).awsSecurityTokenServiceClient(awsCredentialView);

        StsClient delegatorStsClient = mockSuccessfulStsClient();
        doReturn(delegatorStsClient).when(underTest).buildDelegatorStsClient(awsCredentialView);
        setUpCredentialView();

        AwsSessionCredentials result = underTest.retrieveSessionCredentials(awsCredentialView);

        assertNotNull(result);
        assertEquals("accessKey", result.accessKeyId());
        verify(failingStsClient, times(1)).assumeRole(any(AssumeRoleRequest.class));
        verify(delegatorStsClient, times(1)).assumeRole(any(AssumeRoleRequest.class));
    }

    @Test
    void retrieveSessionCredentialsThrowsWhenSaFailsAndNoDelegator() {
        setUpDelegatorConfig("");
        StsClient failingStsClient = mock(StsClient.class);
        when(failingStsClient.assumeRole(any(AssumeRoleRequest.class)))
                .thenThrow(SdkException.builder().message("Access Denied").build());
        doReturn(failingStsClient).when(underTest).awsSecurityTokenServiceClient(awsCredentialView);
        setUpCredentialView();

        assertThrows(SdkException.class, () -> underTest.retrieveSessionCredentials(awsCredentialView));
    }

    @Test
    void createStsAssumeRoleCredentialsProviderReturnProviderWhenNoDelegatorConfigured() {
        setUpDelegatorConfig("");
        doReturn(mock(StsClient.class)).when(underTest).awsSecurityTokenServiceClient(awsCredentialView);
        setUpCredentialView();

        StsAssumeRoleCredentialsProvider result = underTest.createStsAssumeRoleCredentialsProvider(awsCredentialView);

        assertNotNull(result);
        verify(underTest, times(0)).buildDelegatorStsClient(any());
    }

    @Test
    void createStsAssumeRoleCredentialsProviderReturnDirectProviderWhenSaCanAssume() {
        setUpDelegatorConfig(DELEGATOR_ROLE_ARN);
        doReturn(mockSuccessfulStsClient()).when(underTest).awsSecurityTokenServiceClient(awsCredentialView);
        setUpCredentialView();

        StsAssumeRoleCredentialsProvider result = underTest.createStsAssumeRoleCredentialsProvider(awsCredentialView);

        assertNotNull(result);
        verify(underTest, times(0)).buildDelegatorStsClient(any());
    }

    @Test
    void createStsAssumeRoleCredentialsProviderFallsToDelegatorWhenSaCannotAssume() {
        setUpDelegatorConfig(DELEGATOR_ROLE_ARN);
        StsClient failingStsClient = mock(StsClient.class);
        when(failingStsClient.assumeRole(any(AssumeRoleRequest.class)))
                .thenThrow(SdkException.builder().message("Access Denied").build());
        doReturn(failingStsClient).when(underTest).awsSecurityTokenServiceClient(awsCredentialView);
        doReturn(mock(StsClient.class)).when(underTest).buildDelegatorStsClient(awsCredentialView);
        setUpCredentialView();

        StsAssumeRoleCredentialsProvider result = underTest.createStsAssumeRoleCredentialsProvider(awsCredentialView);

        assertNotNull(result);
        verify(underTest, times(1)).buildDelegatorStsClient(awsCredentialView);
    }

    @Test
    void retrieveSessionCredentialsWithoutExternalIdFallsBackToDelegator() {
        setUpDelegatorConfig(DELEGATOR_ROLE_ARN);
        StsClient failingStsClient = mock(StsClient.class);
        when(failingStsClient.assumeRole(any(AssumeRoleRequest.class)))
                .thenThrow(SdkException.builder().message("Access Denied").build());
        doReturn(failingStsClient).when(underTest).awsSecurityTokenServiceClient(awsCredentialView);

        StsClient delegatorStsClient = mockSuccessfulStsClient();
        doReturn(delegatorStsClient).when(underTest).buildDelegatorStsClient(awsCredentialView);
        when(awsCredentialView.getRoleArn()).thenReturn(ROLE_ARN);

        AwsSessionCredentials result = underTest.retrieveSessionCredentialsWithoutExternalId(awsCredentialView);

        assertNotNull(result);
        verify(delegatorStsClient, times(1)).assumeRole(any(AssumeRoleRequest.class));
    }

    private String setUpMocks(boolean fipsEnabled, boolean onGovCloud) {
        String defaultRegion = Region.EU_CENTRAL_1.toString();
        ReflectionTestUtils.setField(underTest, "fipsEnabled", fipsEnabled);
        when(awsEnvironmentVariableChecker.isAwsAccessKeyAvailable(awsCredentialView)).thenReturn(Boolean.FALSE);
        when(awsDefaultZoneProvider.getDefaultZone(awsCredentialView)).thenReturn(defaultRegion);
        lenient().when(awsCredentialView.isGovernmentCloudEnabled()).thenReturn(onGovCloud);
        return defaultRegion;
    }

    private void setUpDelegatorConfig(String delegatorArn) {
        ReflectionTestUtils.setField(underTest, "delegatorRoleArn", delegatorArn);
        ReflectionTestUtils.setField(underTest, "delegatorRoleSessionName", "cdp-delegator-provisioning");
    }

    private void setUpCredentialView() {
        lenient().when(awsCredentialView.getRoleArn()).thenReturn(ROLE_ARN);
        lenient().when(awsCredentialView.getExternalId()).thenReturn(EXTERNAL_ID);
    }

    private StsClient mockSuccessfulStsClient() {
        StsClient stsClient = mock(StsClient.class);
        Credentials credentials = Credentials.builder()
                .accessKeyId("accessKey")
                .secretAccessKey("secretKey")
                .sessionToken("sessionToken")
                .expiration(Instant.now().plusSeconds(3600))
                .build();
        when(stsClient.assumeRole(any(AssumeRoleRequest.class)))
                .thenReturn(AssumeRoleResponse.builder().credentials(credentials).build());
        return stsClient;
    }
}
