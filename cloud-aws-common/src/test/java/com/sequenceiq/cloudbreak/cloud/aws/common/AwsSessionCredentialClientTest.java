package com.sequenceiq.cloudbreak.cloud.aws.common;

import static com.sequenceiq.cloudbreak.cloud.aws.common.AssumeRoleType.DEFAULT_CREDENTIAL_CHAIN;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AwsApacheClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.cloud.credential.CredentialAssumeRoleTypePersister;

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

    private static final String CREDENTIAL_CRN = "crn:cdp:environments:us-west-1:tenant:credential:12345";

    private static final String ACCOUNT_ID = "tenant";

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

    @Mock
    private EntitlementService entitlementService;

    @Mock
    private CredentialAssumeRoleTypePersister credentialAssumeRoleTypePersister;

    @BeforeEach
    void setUp() {
        // @InjectMocks cannot populate an Optional<T> field from a raw mock, so wrap and set it explicitly.
        ReflectionTestUtils.setField(underTest, "credentialAssumeRoleTypePersister", Optional.of(credentialAssumeRoleTypePersister));
    }

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
    void buildDelegatorStsClientAppliesEndpointOverrideToInnerAndOuterClientWhenNotGovCloud() {
        // Regression guard for the delegator "first hop" STS client: it must be built through baseStsClientBuilder,
        // exactly like the outer client, so the FIPS endpoint override AND the retry policy apply to both. Before the
        // fix the inner client used a bare StsClient.builder(), so getEndpointConfiguration was invoked only once
        // (outer only). Two invocations proves both clients now share baseStsClientBuilder.
        String defaultRegion = setUpMocks(Boolean.FALSE, Boolean.FALSE);
        setUpDelegatorConfig(DELEGATOR_ROLE_ARN);

        StsClient actual = underTest.buildDelegatorStsClient(awsCredentialView);

        assertNotNull(actual);
        verify(underTest, times(2)).getEndpointConfiguration(defaultRegion);
    }

    @Test
    void buildDelegatorStsClientAppliesEndpointOverrideToInnerAndOuterClientWhenFipsButNotGovCloud() {
        // FIPS enabled on a non-GovCloud credential still overrides the endpoint on both the inner and outer clients.
        String defaultRegion = setUpMocks(Boolean.TRUE, Boolean.FALSE);
        setUpDelegatorConfig(DELEGATOR_ROLE_ARN);

        StsClient actual = underTest.buildDelegatorStsClient(awsCredentialView);

        assertNotNull(actual);
        verify(underTest, times(2)).getEndpointConfiguration(defaultRegion);
    }

    @Test
    void buildDelegatorStsClientSkipsEndpointOverrideOnBothClientsWhenFipsAndGovCloud() {
        // In FIPS GovCloud neither the inner (delegator-assume) nor the outer client may override the STS endpoint.
        String defaultRegion = setUpMocks(Boolean.TRUE, Boolean.TRUE);
        setUpDelegatorConfig(DELEGATOR_ROLE_ARN);

        StsClient actual = underTest.buildDelegatorStsClient(awsCredentialView);

        assertNotNull(actual);
        verify(underTest, times(0)).getEndpointConfiguration(defaultRegion);
    }

    @Test
    void retrieveSessionCredentialsUsesAccessKeyDirectlyWhenAccessKeyConfigured() {
        setUpDelegatorConfig(DELEGATOR_ROLE_ARN);
        StsClient mockStsClient = mockSuccessfulStsClient();
        doReturn(mockStsClient).when(underTest).awsSecurityTokenServiceClient(awsCredentialView);
        setUpCredentialView();
        when(awsEnvironmentVariableChecker.isAwsAccessKeyAvailable(awsCredentialView)).thenReturn(true);
        when(awsEnvironmentVariableChecker.isAwsSecretAccessKeyAvailable(awsCredentialView)).thenReturn(true);

        AwsSessionCredentials result = underTest.retrieveSessionCredentials(awsCredentialView);

        assertNotNull(result);
        assertEquals("accessKey", result.accessKeyId());
        verify(mockStsClient, times(1)).assumeRole(any(AssumeRoleRequest.class));
        verify(underTest, never()).buildDelegatorStsClient(any());
        verify(awsCredentialView).setRoleAssumeType(DEFAULT_CREDENTIAL_CHAIN);
        verify(credentialAssumeRoleTypePersister).persistAssumeRoleType(CREDENTIAL_CRN, ACCOUNT_ID, DEFAULT_CREDENTIAL_CHAIN.name());
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
        verify(awsCredentialView).setRoleAssumeType(DEFAULT_CREDENTIAL_CHAIN);
        verify(credentialAssumeRoleTypePersister).persistAssumeRoleType(CREDENTIAL_CRN, ACCOUNT_ID, DEFAULT_CREDENTIAL_CHAIN.name());
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
        when(entitlementService.isAwsDelegatorRoleBasedCredentialEnabled(ACCOUNT_ID)).thenReturn(true);

        AwsSessionCredentials result = underTest.retrieveSessionCredentials(awsCredentialView);

        assertNotNull(result);
        assertEquals("accessKey", result.accessKeyId());
        verify(failingStsClient, times(1)).assumeRole(any(AssumeRoleRequest.class));
        verify(delegatorStsClient, times(1)).assumeRole(any(AssumeRoleRequest.class));
        verify(awsCredentialView).setRoleAssumeType(AssumeRoleType.DELEGATOR);
        verify(credentialAssumeRoleTypePersister).persistAssumeRoleType(CREDENTIAL_CRN, ACCOUNT_ID, AssumeRoleType.DELEGATOR.name());
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
        verify(credentialAssumeRoleTypePersister, never()).persistAssumeRoleType(any(), any(), any());
    }

    @Test
    void retrieveSessionCredentialsThrowsWhenSaFailsAndDelegatorNotEntitled() {
        setUpDelegatorConfig(DELEGATOR_ROLE_ARN);
        StsClient failingStsClient = mock(StsClient.class);
        when(failingStsClient.assumeRole(any(AssumeRoleRequest.class)))
                .thenThrow(SdkException.builder().message("Access Denied").build());
        doReturn(failingStsClient).when(underTest).awsSecurityTokenServiceClient(awsCredentialView);
        setUpCredentialView();
        when(entitlementService.isAwsDelegatorRoleBasedCredentialEnabled(ACCOUNT_ID)).thenReturn(false);

        assertThrows(SdkException.class, () -> underTest.retrieveSessionCredentials(awsCredentialView));
        verify(underTest, never()).buildDelegatorStsClient(any());
        verify(credentialAssumeRoleTypePersister, never()).persistAssumeRoleType(any(), any(), any());
    }

    @Test
    void retrieveSessionCredentialsUsesStoredDelegatorDirectly() {
        setUpDelegatorConfig(DELEGATOR_ROLE_ARN);
        when(awsCredentialView.getRoleAssumeType()).thenReturn(AssumeRoleType.DELEGATOR);
        when(entitlementService.isAwsDelegatorRoleBasedCredentialEnabled(ACCOUNT_ID)).thenReturn(true);
        StsClient delegatorStsClient = mockSuccessfulStsClient();
        doReturn(delegatorStsClient).when(underTest).buildDelegatorStsClient(awsCredentialView);
        setUpCredentialView();

        AwsSessionCredentials result = underTest.retrieveSessionCredentials(awsCredentialView);

        assertNotNull(result);
        assertEquals("accessKey", result.accessKeyId());
        verify(underTest, never()).awsSecurityTokenServiceClient(any());
        verify(delegatorStsClient, times(1)).assumeRole(any(AssumeRoleRequest.class));
        verify(credentialAssumeRoleTypePersister, never()).persistAssumeRoleType(any(), any(), any());
    }

    @Test
    void retrieveSessionCredentialsStoredDelegatorFailsFallsBackToSaAndResetsType() {
        setUpDelegatorConfig(DELEGATOR_ROLE_ARN);
        when(awsCredentialView.getRoleAssumeType()).thenReturn(AssumeRoleType.DELEGATOR);
        when(entitlementService.isAwsDelegatorRoleBasedCredentialEnabled(ACCOUNT_ID)).thenReturn(true);
        StsClient failingDelegatorStsClient = mock(StsClient.class);
        when(failingDelegatorStsClient.assumeRole(any(AssumeRoleRequest.class)))
                .thenThrow(SdkException.builder().message("Access Denied").build());
        doReturn(failingDelegatorStsClient).when(underTest).buildDelegatorStsClient(awsCredentialView);
        StsClient saStsClient = mockSuccessfulStsClient();
        doReturn(saStsClient).when(underTest).awsSecurityTokenServiceClient(awsCredentialView);
        setUpCredentialView();

        AwsSessionCredentials result = underTest.retrieveSessionCredentials(awsCredentialView);

        assertNotNull(result);
        assertEquals("accessKey", result.accessKeyId());
        verify(failingDelegatorStsClient, times(1)).assumeRole(any(AssumeRoleRequest.class));
        verify(saStsClient, times(1)).assumeRole(any(AssumeRoleRequest.class));
        verify(awsCredentialView).setRoleAssumeType(DEFAULT_CREDENTIAL_CHAIN);
        verify(credentialAssumeRoleTypePersister).persistAssumeRoleType(CREDENTIAL_CRN, ACCOUNT_ID, DEFAULT_CREDENTIAL_CHAIN.name());
    }

    @Test
    void retrieveSessionCredentialsStoredDelegatorFailsAndSaAlsoFailsThrows() {
        setUpDelegatorConfig(DELEGATOR_ROLE_ARN);
        when(awsCredentialView.getRoleAssumeType()).thenReturn(AssumeRoleType.DELEGATOR);
        when(entitlementService.isAwsDelegatorRoleBasedCredentialEnabled(ACCOUNT_ID)).thenReturn(true);
        StsClient failingDelegatorStsClient = mock(StsClient.class);
        when(failingDelegatorStsClient.assumeRole(any(AssumeRoleRequest.class)))
                .thenThrow(SdkException.builder().message("Delegator denied").build());
        doReturn(failingDelegatorStsClient).when(underTest).buildDelegatorStsClient(awsCredentialView);
        StsClient failingSaStsClient = mock(StsClient.class);
        when(failingSaStsClient.assumeRole(any(AssumeRoleRequest.class)))
                .thenThrow(SdkException.builder().message("SA denied").build());
        doReturn(failingSaStsClient).when(underTest).awsSecurityTokenServiceClient(awsCredentialView);
        setUpCredentialView();

        assertThrows(SdkException.class, () -> underTest.retrieveSessionCredentials(awsCredentialView));
        verify(awsCredentialView, never()).setRoleAssumeType(DEFAULT_CREDENTIAL_CHAIN);
        verify(credentialAssumeRoleTypePersister, never()).persistAssumeRoleType(any(), any(), any());
    }

    @Test
    void retrieveSessionCredentialsUsesStoredSaRoleDirectly() {
        setUpDelegatorConfig(DELEGATOR_ROLE_ARN);
        when(awsCredentialView.getRoleAssumeType()).thenReturn(DEFAULT_CREDENTIAL_CHAIN);
        StsClient mockStsClient = mockSuccessfulStsClient();
        doReturn(mockStsClient).when(underTest).awsSecurityTokenServiceClient(awsCredentialView);
        setUpCredentialView();

        AwsSessionCredentials result = underTest.retrieveSessionCredentials(awsCredentialView);

        assertNotNull(result);
        assertEquals("accessKey", result.accessKeyId());
        verify(mockStsClient, times(1)).assumeRole(any(AssumeRoleRequest.class));
        verify(underTest, never()).buildDelegatorStsClient(any());
        // stored type already SA_ROLE and re-confirmed SA_ROLE -> nothing changed, no persist
        verify(credentialAssumeRoleTypePersister, never()).persistAssumeRoleType(any(), any(), any());
    }

    @Test
    void retrieveSessionCredentialsStoredSaRoleFailsFallsToDelegator() {
        setUpDelegatorConfig(DELEGATOR_ROLE_ARN);
        when(awsCredentialView.getRoleAssumeType()).thenReturn(DEFAULT_CREDENTIAL_CHAIN);
        StsClient failingStsClient = mock(StsClient.class);
        when(failingStsClient.assumeRole(any(AssumeRoleRequest.class)))
                .thenThrow(SdkException.builder().message("Access Denied").build());
        doReturn(failingStsClient).when(underTest).awsSecurityTokenServiceClient(awsCredentialView);

        StsClient delegatorStsClient = mockSuccessfulStsClient();
        doReturn(delegatorStsClient).when(underTest).buildDelegatorStsClient(awsCredentialView);
        setUpCredentialView();
        when(entitlementService.isAwsDelegatorRoleBasedCredentialEnabled(ACCOUNT_ID)).thenReturn(true);

        AwsSessionCredentials result = underTest.retrieveSessionCredentials(awsCredentialView);

        assertNotNull(result);
        assertEquals("accessKey", result.accessKeyId());
        verify(failingStsClient, times(1)).assumeRole(any(AssumeRoleRequest.class));
        verify(delegatorStsClient, times(1)).assumeRole(any(AssumeRoleRequest.class));
        verify(awsCredentialView).setRoleAssumeType(AssumeRoleType.DELEGATOR);
        verify(credentialAssumeRoleTypePersister).persistAssumeRoleType(CREDENTIAL_CRN, ACCOUNT_ID, AssumeRoleType.DELEGATOR.name());
    }

    @Test
    void retrieveSessionCredentialsStoredSaRoleFailsNoDelegatorThrows() {
        setUpDelegatorConfig("");
        when(awsCredentialView.getRoleAssumeType()).thenReturn(DEFAULT_CREDENTIAL_CHAIN);
        StsClient failingStsClient = mock(StsClient.class);
        when(failingStsClient.assumeRole(any(AssumeRoleRequest.class)))
                .thenThrow(SdkException.builder().message("Access Denied").build());
        doReturn(failingStsClient).when(underTest).awsSecurityTokenServiceClient(awsCredentialView);
        setUpCredentialView();

        assertThrows(SdkException.class, () -> underTest.retrieveSessionCredentials(awsCredentialView));
        verify(underTest, never()).buildDelegatorStsClient(any());
        verify(credentialAssumeRoleTypePersister, never()).persistAssumeRoleType(any(), any(), any());
    }

    @Test
    void createStsAssumeRoleCredentialsProviderUsesAccessKeyDirectlyWhenAccessKeyConfigured() {
        setUpDelegatorConfig(DELEGATOR_ROLE_ARN);
        doReturn(mock(StsClient.class)).when(underTest).awsSecurityTokenServiceClient(awsCredentialView);
        setUpCredentialView();
        when(awsEnvironmentVariableChecker.isAwsAccessKeyAvailable(awsCredentialView)).thenReturn(true);
        when(awsEnvironmentVariableChecker.isAwsSecretAccessKeyAvailable(awsCredentialView)).thenReturn(true);

        StsAssumeRoleCredentialsProvider result = underTest.createStsAssumeRoleCredentialsProvider(awsCredentialView);

        assertNotNull(result);
        verify(underTest, never()).buildDelegatorStsClient(any());
        verify(awsCredentialView).setRoleAssumeType(DEFAULT_CREDENTIAL_CHAIN);
        verify(credentialAssumeRoleTypePersister).persistAssumeRoleType(CREDENTIAL_CRN, ACCOUNT_ID, DEFAULT_CREDENTIAL_CHAIN.name());
    }

    @Test
    void createStsAssumeRoleCredentialsProviderReturnProviderWhenNoDelegatorConfigured() {
        setUpDelegatorConfig("");
        doReturn(mock(StsClient.class)).when(underTest).awsSecurityTokenServiceClient(awsCredentialView);
        setUpCredentialView();

        StsAssumeRoleCredentialsProvider result = underTest.createStsAssumeRoleCredentialsProvider(awsCredentialView);

        assertNotNull(result);
        verify(underTest, never()).buildDelegatorStsClient(any());
        verify(awsCredentialView).setRoleAssumeType(DEFAULT_CREDENTIAL_CHAIN);
        verify(credentialAssumeRoleTypePersister).persistAssumeRoleType(CREDENTIAL_CRN, ACCOUNT_ID, DEFAULT_CREDENTIAL_CHAIN.name());
    }

    @Test
    void createStsAssumeRoleCredentialsProviderReturnDirectProviderWhenSaCanAssume() {
        setUpDelegatorConfig(DELEGATOR_ROLE_ARN);
        doReturn(mockSuccessfulStsClient()).when(underTest).awsSecurityTokenServiceClient(awsCredentialView);
        setUpCredentialView();
        when(entitlementService.isAwsDelegatorRoleBasedCredentialEnabled(ACCOUNT_ID)).thenReturn(true);

        StsAssumeRoleCredentialsProvider result = underTest.createStsAssumeRoleCredentialsProvider(awsCredentialView);

        assertNotNull(result);
        verify(underTest, never()).buildDelegatorStsClient(any());
        verify(awsCredentialView).setRoleAssumeType(DEFAULT_CREDENTIAL_CHAIN);
        verify(credentialAssumeRoleTypePersister).persistAssumeRoleType(CREDENTIAL_CRN, ACCOUNT_ID, DEFAULT_CREDENTIAL_CHAIN.name());
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
        when(entitlementService.isAwsDelegatorRoleBasedCredentialEnabled(ACCOUNT_ID)).thenReturn(true);

        StsAssumeRoleCredentialsProvider result = underTest.createStsAssumeRoleCredentialsProvider(awsCredentialView);

        assertNotNull(result);
        verify(underTest, times(1)).buildDelegatorStsClient(awsCredentialView);
        verify(awsCredentialView).setRoleAssumeType(AssumeRoleType.DELEGATOR);
        verify(credentialAssumeRoleTypePersister).persistAssumeRoleType(CREDENTIAL_CRN, ACCOUNT_ID, AssumeRoleType.DELEGATOR.name());
    }

    @Test
    void createStsAssumeRoleCredentialsProviderUsesStoredDelegatorDirectly() {
        setUpDelegatorConfig(DELEGATOR_ROLE_ARN);
        when(awsCredentialView.getRoleAssumeType()).thenReturn(AssumeRoleType.DELEGATOR);
        when(entitlementService.isAwsDelegatorRoleBasedCredentialEnabled(ACCOUNT_ID)).thenReturn(true);
        doReturn(mockSuccessfulStsClient()).when(underTest).buildDelegatorStsClient(awsCredentialView);
        setUpCredentialView();

        StsAssumeRoleCredentialsProvider result = underTest.createStsAssumeRoleCredentialsProvider(awsCredentialView);

        assertNotNull(result);
        verify(underTest, times(1)).buildDelegatorStsClient(awsCredentialView);
        verify(underTest, never()).awsSecurityTokenServiceClient(any());
        verify(credentialAssumeRoleTypePersister, never()).persistAssumeRoleType(any(), any(), any());
    }

    @Test
    void createStsAssumeRoleCredentialsProviderStoredDelegatorFailsFallsBackToSaAndResetsType() {
        setUpDelegatorConfig(DELEGATOR_ROLE_ARN);
        when(awsCredentialView.getRoleAssumeType()).thenReturn(AssumeRoleType.DELEGATOR);
        when(entitlementService.isAwsDelegatorRoleBasedCredentialEnabled(ACCOUNT_ID)).thenReturn(true);
        StsClient failingDelegatorStsClient = mock(StsClient.class);
        when(failingDelegatorStsClient.assumeRole(any(AssumeRoleRequest.class)))
                .thenThrow(SdkException.builder().message("Access Denied").build());
        doReturn(failingDelegatorStsClient).when(underTest).buildDelegatorStsClient(awsCredentialView);
        doReturn(mockSuccessfulStsClient()).when(underTest).awsSecurityTokenServiceClient(awsCredentialView);
        setUpCredentialView();

        StsAssumeRoleCredentialsProvider result = underTest.createStsAssumeRoleCredentialsProvider(awsCredentialView);

        assertNotNull(result);
        verify(underTest, times(1)).buildDelegatorStsClient(awsCredentialView);
        verify(underTest, times(1)).awsSecurityTokenServiceClient(awsCredentialView);
        verify(awsCredentialView).setRoleAssumeType(DEFAULT_CREDENTIAL_CHAIN);
        verify(credentialAssumeRoleTypePersister).persistAssumeRoleType(CREDENTIAL_CRN, ACCOUNT_ID, DEFAULT_CREDENTIAL_CHAIN.name());
    }

    @Test
    void createStsAssumeRoleCredentialsProviderStoredDelegatorFailsAndSaAlsoFailsThrows() {
        setUpDelegatorConfig(DELEGATOR_ROLE_ARN);
        when(awsCredentialView.getRoleAssumeType()).thenReturn(AssumeRoleType.DELEGATOR);
        when(entitlementService.isAwsDelegatorRoleBasedCredentialEnabled(ACCOUNT_ID)).thenReturn(true);
        StsClient failingDelegatorStsClient = mock(StsClient.class);
        when(failingDelegatorStsClient.assumeRole(any(AssumeRoleRequest.class)))
                .thenThrow(SdkException.builder().message("Delegator denied").build());
        doReturn(failingDelegatorStsClient).when(underTest).buildDelegatorStsClient(awsCredentialView);
        StsClient failingSaStsClient = mock(StsClient.class);
        when(failingSaStsClient.assumeRole(any(AssumeRoleRequest.class)))
                .thenThrow(SdkException.builder().message("SA denied").build());
        doReturn(failingSaStsClient).when(underTest).awsSecurityTokenServiceClient(awsCredentialView);
        setUpCredentialView();

        assertThrows(SdkException.class, () -> underTest.createStsAssumeRoleCredentialsProvider(awsCredentialView));
        verify(awsCredentialView, never()).setRoleAssumeType(DEFAULT_CREDENTIAL_CHAIN);
        verify(credentialAssumeRoleTypePersister, never()).persistAssumeRoleType(any(), any(), any());
    }

    @Test
    void createStsAssumeRoleCredentialsProviderUsesStoredSaRoleDirectly() {
        setUpDelegatorConfig(DELEGATOR_ROLE_ARN);
        when(awsCredentialView.getRoleAssumeType()).thenReturn(DEFAULT_CREDENTIAL_CHAIN);
        when(entitlementService.isAwsDelegatorRoleBasedCredentialEnabled(ACCOUNT_ID)).thenReturn(true);
        doReturn(mockSuccessfulStsClient()).when(underTest).awsSecurityTokenServiceClient(awsCredentialView);
        setUpCredentialView();

        StsAssumeRoleCredentialsProvider result = underTest.createStsAssumeRoleCredentialsProvider(awsCredentialView);

        assertNotNull(result);
        verify(underTest, never()).buildDelegatorStsClient(any());
        // stored type already SA_ROLE and re-confirmed SA_ROLE -> nothing changed, no persist
        verify(credentialAssumeRoleTypePersister, never()).persistAssumeRoleType(any(), any(), any());
    }

    @Test
    void createStsAssumeRoleCredentialsProviderStoredSaRoleFailsFallsToDelegator() {
        setUpDelegatorConfig(DELEGATOR_ROLE_ARN);
        when(awsCredentialView.getRoleAssumeType()).thenReturn(DEFAULT_CREDENTIAL_CHAIN);
        StsClient failingStsClient = mock(StsClient.class);
        when(failingStsClient.assumeRole(any(AssumeRoleRequest.class)))
                .thenThrow(SdkException.builder().message("Access Denied").build());
        doReturn(failingStsClient).when(underTest).awsSecurityTokenServiceClient(awsCredentialView);
        doReturn(mock(StsClient.class)).when(underTest).buildDelegatorStsClient(awsCredentialView);
        setUpCredentialView();
        when(entitlementService.isAwsDelegatorRoleBasedCredentialEnabled(ACCOUNT_ID)).thenReturn(true);

        StsAssumeRoleCredentialsProvider result = underTest.createStsAssumeRoleCredentialsProvider(awsCredentialView);

        assertNotNull(result);
        verify(underTest, times(1)).buildDelegatorStsClient(awsCredentialView);
        verify(awsCredentialView).setRoleAssumeType(AssumeRoleType.DELEGATOR);
        verify(credentialAssumeRoleTypePersister).persistAssumeRoleType(CREDENTIAL_CRN, ACCOUNT_ID, AssumeRoleType.DELEGATOR.name());
    }

    @Test
    void createStsAssumeRoleCredentialsProviderReturnsSaProviderWithoutProbeWhenDelegatorNotEntitled() {
        // Unlike the eager retrieveSessionCredentials path, provider creation is lazy: when the delegator is not
        // entitled there is no fallback to weigh, so the SA provider is returned unprobed (no resolveCredentials,
        // hence no SdkException here) and credential resolution is deferred to actual use.
        setUpDelegatorConfig(DELEGATOR_ROLE_ARN);
        doReturn(mock(StsClient.class)).when(underTest).awsSecurityTokenServiceClient(awsCredentialView);
        setUpCredentialView();
        when(entitlementService.isAwsDelegatorRoleBasedCredentialEnabled(ACCOUNT_ID)).thenReturn(false);

        StsAssumeRoleCredentialsProvider result = underTest.createStsAssumeRoleCredentialsProvider(awsCredentialView);

        assertNotNull(result);
        verify(underTest, never()).buildDelegatorStsClient(any());
        verify(awsCredentialView).setRoleAssumeType(DEFAULT_CREDENTIAL_CHAIN);
        verify(credentialAssumeRoleTypePersister).persistAssumeRoleType(CREDENTIAL_CRN, ACCOUNT_ID, DEFAULT_CREDENTIAL_CHAIN.name());
    }

    @Test
    void retrieveSessionCredentialsWithoutExternalIdUsesAccessKeyDirectly() {
        setUpDelegatorConfig(DELEGATOR_ROLE_ARN);
        StsClient mockStsClient = mockSuccessfulStsClient();
        doReturn(mockStsClient).when(underTest).awsSecurityTokenServiceClient(awsCredentialView);
        when(awsCredentialView.getRoleArn()).thenReturn(ROLE_ARN);
        when(awsEnvironmentVariableChecker.isAwsAccessKeyAvailable(awsCredentialView)).thenReturn(true);
        when(awsEnvironmentVariableChecker.isAwsSecretAccessKeyAvailable(awsCredentialView)).thenReturn(true);

        AwsSessionCredentials result = underTest.retrieveSessionCredentialsWithoutExternalIdForValidationOnly(awsCredentialView);

        assertNotNull(result);
        assertEquals("accessKey", result.accessKeyId());
        verify(underTest, never()).buildDelegatorStsClient(any());
        verify(awsCredentialView).setRoleAssumeType(DEFAULT_CREDENTIAL_CHAIN);
        verify(credentialAssumeRoleTypePersister, never()).persistAssumeRoleType(any(), any(), any());
    }

    @Test
    void retrieveSessionCredentialsWithoutExternalIdSucceedsWithSaRole() {
        setUpDelegatorConfig(DELEGATOR_ROLE_ARN);
        StsClient mockStsClient = mockSuccessfulStsClient();
        doReturn(mockStsClient).when(underTest).awsSecurityTokenServiceClient(awsCredentialView);
        when(awsCredentialView.getRoleArn()).thenReturn(ROLE_ARN);

        AwsSessionCredentials result = underTest.retrieveSessionCredentialsWithoutExternalIdForValidationOnly(awsCredentialView);

        assertNotNull(result);
        assertEquals("accessKey", result.accessKeyId());
        verify(underTest, never()).buildDelegatorStsClient(any());
        verify(awsCredentialView).setRoleAssumeType(DEFAULT_CREDENTIAL_CHAIN);
        verify(credentialAssumeRoleTypePersister, never()).persistAssumeRoleType(any(), any(), any());
    }

    @Test
    void retrieveSessionCredentialsWithoutExternalIdUsesStoredDelegatorDirectly() {
        setUpDelegatorConfig(DELEGATOR_ROLE_ARN);
        when(awsCredentialView.getRoleAssumeType()).thenReturn(AssumeRoleType.DELEGATOR);
        when(awsCredentialView.getAccountId()).thenReturn(ACCOUNT_ID);
        when(entitlementService.isAwsDelegatorRoleBasedCredentialEnabled(ACCOUNT_ID)).thenReturn(true);
        StsClient delegatorStsClient = mockSuccessfulStsClient();
        doReturn(delegatorStsClient).when(underTest).buildDelegatorStsClient(awsCredentialView);
        when(awsCredentialView.getRoleArn()).thenReturn(ROLE_ARN);

        AwsSessionCredentials result = underTest.retrieveSessionCredentialsWithoutExternalIdForValidationOnly(awsCredentialView);

        assertNotNull(result);
        verify(underTest, never()).awsSecurityTokenServiceClient(any());
        verify(delegatorStsClient, times(1)).assumeRole(any(AssumeRoleRequest.class));
        verify(credentialAssumeRoleTypePersister, never()).persistAssumeRoleType(any(), any(), any());
    }

    @Test
    void retrieveSessionCredentialsAccessKeyTakesPriorityOverStoredDelegator() {
        setUpDelegatorConfig(DELEGATOR_ROLE_ARN);
        lenient().when(awsCredentialView.getRoleAssumeType()).thenReturn(AssumeRoleType.DELEGATOR);
        StsClient mockStsClient = mockSuccessfulStsClient();
        doReturn(mockStsClient).when(underTest).awsSecurityTokenServiceClient(awsCredentialView);
        setUpCredentialView();
        when(awsEnvironmentVariableChecker.isAwsAccessKeyAvailable(awsCredentialView)).thenReturn(true);
        when(awsEnvironmentVariableChecker.isAwsSecretAccessKeyAvailable(awsCredentialView)).thenReturn(true);

        AwsSessionCredentials result = underTest.retrieveSessionCredentials(awsCredentialView);

        assertNotNull(result);
        verify(underTest, never()).buildDelegatorStsClient(any());
        verify(awsCredentialView).setRoleAssumeType(DEFAULT_CREDENTIAL_CHAIN);
        verify(credentialAssumeRoleTypePersister).persistAssumeRoleType(CREDENTIAL_CRN, ACCOUNT_ID, DEFAULT_CREDENTIAL_CHAIN.name());
    }

    @Test
    void createStsAssumeRoleCredentialsProviderAccessKeyTakesPriorityOverStoredDelegator() {
        setUpDelegatorConfig(DELEGATOR_ROLE_ARN);
        lenient().when(awsCredentialView.getRoleAssumeType()).thenReturn(AssumeRoleType.DELEGATOR);
        doReturn(mock(StsClient.class)).when(underTest).awsSecurityTokenServiceClient(awsCredentialView);
        setUpCredentialView();
        when(awsEnvironmentVariableChecker.isAwsAccessKeyAvailable(awsCredentialView)).thenReturn(true);
        when(awsEnvironmentVariableChecker.isAwsSecretAccessKeyAvailable(awsCredentialView)).thenReturn(true);

        StsAssumeRoleCredentialsProvider result = underTest.createStsAssumeRoleCredentialsProvider(awsCredentialView);

        assertNotNull(result);
        verify(underTest, never()).buildDelegatorStsClient(any());
        verify(awsCredentialView).setRoleAssumeType(DEFAULT_CREDENTIAL_CHAIN);
        verify(credentialAssumeRoleTypePersister).persistAssumeRoleType(CREDENTIAL_CRN, ACCOUNT_ID, DEFAULT_CREDENTIAL_CHAIN.name());
    }

    @Test
    void retrieveSessionCredentialsWithoutExternalIdThrowsWhenSaFailsAndNoDelegator() {
        setUpDelegatorConfig("");
        StsClient failingStsClient = mock(StsClient.class);
        when(failingStsClient.assumeRole(any(AssumeRoleRequest.class)))
                .thenThrow(SdkException.builder().message("Access Denied").build());
        doReturn(failingStsClient).when(underTest).awsSecurityTokenServiceClient(awsCredentialView);
        when(awsCredentialView.getRoleArn()).thenReturn(ROLE_ARN);

        assertThrows(SdkException.class, () -> underTest.retrieveSessionCredentialsWithoutExternalIdForValidationOnly(awsCredentialView));
        verify(underTest, never()).buildDelegatorStsClient(any());
        verify(credentialAssumeRoleTypePersister, never()).persistAssumeRoleType(any(), any(), any());
    }

    @Test
    void createStsAssumeRoleCredentialsProviderStoredDelegatorNotEntitledFallsToSaProbe() {
        setUpDelegatorConfig(DELEGATOR_ROLE_ARN);
        when(awsCredentialView.getRoleAssumeType()).thenReturn(AssumeRoleType.DELEGATOR);
        when(entitlementService.isAwsDelegatorRoleBasedCredentialEnabled(ACCOUNT_ID)).thenReturn(false);
        doReturn(mock(StsClient.class)).when(underTest).awsSecurityTokenServiceClient(awsCredentialView);
        setUpCredentialView();

        StsAssumeRoleCredentialsProvider result = underTest.createStsAssumeRoleCredentialsProvider(awsCredentialView);

        assertNotNull(result);
        verify(underTest, never()).buildDelegatorStsClient(any());
        verify(awsCredentialView).setRoleAssumeType(DEFAULT_CREDENTIAL_CHAIN);
        verify(credentialAssumeRoleTypePersister).persistAssumeRoleType(CREDENTIAL_CRN, ACCOUNT_ID, DEFAULT_CREDENTIAL_CHAIN.name());
    }

    @Test
    void retrieveSessionCredentialsStoredDelegatorNotEntitledFallsToSaProbe() {
        setUpDelegatorConfig(DELEGATOR_ROLE_ARN);
        when(awsCredentialView.getRoleAssumeType()).thenReturn(AssumeRoleType.DELEGATOR);
        when(entitlementService.isAwsDelegatorRoleBasedCredentialEnabled(ACCOUNT_ID)).thenReturn(false);
        StsClient mockStsClient = mockSuccessfulStsClient();
        doReturn(mockStsClient).when(underTest).awsSecurityTokenServiceClient(awsCredentialView);
        setUpCredentialView();

        AwsSessionCredentials result = underTest.retrieveSessionCredentials(awsCredentialView);

        assertNotNull(result);
        assertEquals("accessKey", result.accessKeyId());
        verify(underTest, never()).buildDelegatorStsClient(any());
        verify(awsCredentialView).setRoleAssumeType(DEFAULT_CREDENTIAL_CHAIN);
        verify(credentialAssumeRoleTypePersister).persistAssumeRoleType(CREDENTIAL_CRN, ACCOUNT_ID, DEFAULT_CREDENTIAL_CHAIN.name());
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
        when(awsCredentialView.getAccountId()).thenReturn(ACCOUNT_ID);
        when(entitlementService.isAwsDelegatorRoleBasedCredentialEnabled(ACCOUNT_ID)).thenReturn(true);

        AwsSessionCredentials result = underTest.retrieveSessionCredentialsWithoutExternalIdForValidationOnly(awsCredentialView);

        assertNotNull(result);
        verify(delegatorStsClient, times(1)).assumeRole(any(AssumeRoleRequest.class));
        verify(awsCredentialView).setRoleAssumeType(AssumeRoleType.DELEGATOR);
        verify(credentialAssumeRoleTypePersister, never()).persistAssumeRoleType(any(), any(), any());
    }

    @Test
    void retrieveSessionCredentialsWithoutExternalIdStoredDelegatorFailsFallsBackToSaWithoutPersist() {
        setUpDelegatorConfig(DELEGATOR_ROLE_ARN);
        when(awsCredentialView.getRoleAssumeType()).thenReturn(AssumeRoleType.DELEGATOR);
        when(awsCredentialView.getAccountId()).thenReturn(ACCOUNT_ID);
        when(entitlementService.isAwsDelegatorRoleBasedCredentialEnabled(ACCOUNT_ID)).thenReturn(true);
        StsClient failingDelegatorStsClient = mock(StsClient.class);
        when(failingDelegatorStsClient.assumeRole(any(AssumeRoleRequest.class)))
                .thenThrow(SdkException.builder().message("Access Denied").build());
        doReturn(failingDelegatorStsClient).when(underTest).buildDelegatorStsClient(awsCredentialView);
        StsClient saStsClient = mockSuccessfulStsClient();
        doReturn(saStsClient).when(underTest).awsSecurityTokenServiceClient(awsCredentialView);
        when(awsCredentialView.getRoleArn()).thenReturn(ROLE_ARN);

        AwsSessionCredentials result = underTest.retrieveSessionCredentialsWithoutExternalIdForValidationOnly(awsCredentialView);

        assertNotNull(result);
        assertEquals("accessKey", result.accessKeyId());
        verify(failingDelegatorStsClient, times(1)).assumeRole(any(AssumeRoleRequest.class));
        verify(saStsClient, times(1)).assumeRole(any(AssumeRoleRequest.class));
        // type is corrected in-memory, but the validation-only path must not persist
        verify(awsCredentialView).setRoleAssumeType(DEFAULT_CREDENTIAL_CHAIN);
        verify(credentialAssumeRoleTypePersister, never()).persistAssumeRoleType(any(), any(), any());
    }

    @Test
    void retrieveSessionCredentialsWithoutExternalIdStoredDelegatorFailsAndSaAlsoFailsThrows() {
        setUpDelegatorConfig(DELEGATOR_ROLE_ARN);
        when(awsCredentialView.getRoleAssumeType()).thenReturn(AssumeRoleType.DELEGATOR);
        when(awsCredentialView.getAccountId()).thenReturn(ACCOUNT_ID);
        when(entitlementService.isAwsDelegatorRoleBasedCredentialEnabled(ACCOUNT_ID)).thenReturn(true);
        StsClient failingDelegatorStsClient = mock(StsClient.class);
        when(failingDelegatorStsClient.assumeRole(any(AssumeRoleRequest.class)))
                .thenThrow(SdkException.builder().message("Delegator denied").build());
        doReturn(failingDelegatorStsClient).when(underTest).buildDelegatorStsClient(awsCredentialView);
        StsClient failingSaStsClient = mock(StsClient.class);
        when(failingSaStsClient.assumeRole(any(AssumeRoleRequest.class)))
                .thenThrow(SdkException.builder().message("SA denied").build());
        doReturn(failingSaStsClient).when(underTest).awsSecurityTokenServiceClient(awsCredentialView);
        when(awsCredentialView.getRoleArn()).thenReturn(ROLE_ARN);

        assertThrows(SdkException.class, () -> underTest.retrieveSessionCredentialsWithoutExternalIdForValidationOnly(awsCredentialView));
        verify(awsCredentialView, never()).setRoleAssumeType(DEFAULT_CREDENTIAL_CHAIN);
        verify(credentialAssumeRoleTypePersister, never()).persistAssumeRoleType(any(), any(), any());
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
        lenient().when(awsCredentialView.getCredentialCrn()).thenReturn(CREDENTIAL_CRN);
        lenient().when(awsCredentialView.getAccountId()).thenReturn(ACCOUNT_ID);
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
