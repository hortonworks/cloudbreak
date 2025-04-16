package com.sequenceiq.cloudbreak.cloud.aws.common;

import static com.google.common.base.Charsets.UTF_8;
import static com.sequenceiq.cloudbreak.cloud.aws.common.AwsCredentialConnector.ROLE_IS_NOT_ASSUMABLE_ERROR_MESSAGE_INDICATOR;
import static com.sequenceiq.cloudbreak.experience.PolicyServiceName.MLX;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.google.common.io.Resources;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.cloud.aws.common.exception.AwsConfusedDeputyException;
import com.sequenceiq.cloudbreak.cloud.aws.common.exception.AwsPermissionMissingException;
import com.sequenceiq.cloudbreak.cloud.aws.common.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.cloud.aws.common.view.AwsCredentialViewProvider;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CDPServicePolicyVerificationResponses;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredentialSettings;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredentialStatus;
import com.sequenceiq.cloudbreak.cloud.model.CredentialStatus;
import com.sequenceiq.cloudbreak.cloud.model.credential.CredentialVerificationContext;
import com.sequenceiq.cloudbreak.cloud.response.CredentialPrerequisitesResponse;
import com.sequenceiq.cloudbreak.common.base64.Base64Util;
import com.sequenceiq.common.model.CredentialType;

import software.amazon.awssdk.core.exception.SdkException;

@ExtendWith(MockitoExtension.class)
public class AwsCredentialConnectorTest {

    private static final CredentialVerificationContext CREDENTIAL_VERIFICATION_CONTEXT = new CredentialVerificationContext(Boolean.FALSE);

    private static final byte PRESENT_ONCE = 1;

    @Mock
    private AuthenticatedContext authenticatedContext;

    @Mock
    private AwsSessionCredentialClient credentialClient;

    @Mock
    private AwsCredentialVerifier awsCredentialVerifier;

    @Mock
    private AwsPlatformParameters awsPlatformParameters;

    @Mock
    private CloudCredential credential;

    @Mock
    private AwsCredentialViewProvider awsCredentialViewProvider;

    @Mock
    private AwsCredentialView credentialView;

    @Mock
    private EntitlementService entitlementService;

    @Mock
    private AwsDefaultRegionSelector defaultRegionSelector;

    @InjectMocks
    private AwsCredentialConnector underTest;

    @BeforeEach
    public void setUp() {
        lenient().when(credential.getCredentialSettings()).thenReturn(new CloudCredentialSettings(true, false));
        lenient().when(authenticatedContext.getCloudCredential()).thenReturn(credential);
        lenient().when(awsCredentialViewProvider.createAwsCredentialView(credential)).thenReturn(credentialView);
    }

    @Test
    public void testVerifyIfRoleBasedCredentialVerificationThrowsSdkBaseExceptionThenFailedStatusShouldReturn()
            throws AwsPermissionMissingException, IOException {
        URL url = Resources.getResource("definitions/aws-environment-minimal-policy.json");
        String awsEnvPolicy = Resources.toString(url, UTF_8);
        String encodedAwsEnvPolicy = Base64Util.encode(awsEnvPolicy);
        String roleArn = "someRoleArn";
        when(credentialView.getRoleArn()).thenReturn(roleArn);
        String exceptionMessageComesFromSdk = "SomethingTerribleHappened!";
        String expectedExceptionMessage = String.format("Unable to verify credential: check if the role '%s' exists and it's created with the correct " +
                "external ID. Cause: '%s'", roleArn, exceptionMessageComesFromSdk);
        Exception sdkException = new IllegalArgumentException(exceptionMessageComesFromSdk);

        when(awsPlatformParameters.getEnvironmentMinimalPoliciesJson()).thenReturn(Map.of(
                PolicyType.PUBLIC, encodedAwsEnvPolicy,
                PolicyType.GOV, encodedAwsEnvPolicy));
        doThrow(sdkException).when(awsCredentialVerifier).validateAws(credentialView, encodedAwsEnvPolicy);
        CloudCredentialStatus result = underTest.verify(authenticatedContext, CREDENTIAL_VERIFICATION_CONTEXT);

        assertNotNull(result);
        assertEquals(CredentialStatus.FAILED, result.getStatus());
        assertEquals(expectedExceptionMessage, result.getStatusReason());
        assertEquals(sdkException, result.getException());

        verify(awsCredentialVerifier, times(1)).validateAws(any(), any());
        verify(awsCredentialVerifier, times(1)).validateAws(credentialView, encodedAwsEnvPolicy);
    }

    @Test
    public void testVerifyIfRoleBasedCredentialVerificationGoesFineThenVerifiedStatusShouldReturn() throws AwsPermissionMissingException {
        String roleArn = "someRoleArn";
        when(credentialView.getRoleArn()).thenReturn(roleArn);
        CloudCredentialStatus result = underTest.verify(authenticatedContext, CREDENTIAL_VERIFICATION_CONTEXT);

        assertNotNull(result);
        assertEquals(CredentialStatus.VERIFIED, result.getStatus());
        assertNull(result.getException());

        verify(awsCredentialVerifier, times(1)).validateAws(any(), any());
    }

    @Test
    public void testVerifyIfRoleBasedCredentialVerificationShouldFailWhenItIsCredentialCreationAndRoleIsAssumableWithoutExternalId()
            throws AwsPermissionMissingException, IOException {
        URL url = Resources.getResource("definitions/aws-environment-minimal-policy.json");
        String awsEnvPolicy = Resources.toString(url, UTF_8);
        String encodedAwsEnvPolicy = Base64Util.encode(awsEnvPolicy);
        String roleArn = "someRoleArn";
        when(credentialView.getRoleArn()).thenReturn(roleArn);
        when(entitlementService.internalTenant(any())).thenReturn(false);

        CloudCredentialStatus result = underTest.verify(authenticatedContext, new CredentialVerificationContext(Boolean.TRUE));

        assertNotNull(result);
        assertEquals(CredentialStatus.FAILED, result.getStatus());
        assertEquals(AwsConfusedDeputyException.class, result.getException().getClass());

        verify(awsCredentialVerifier, times(0)).validateAws(any(), any());
        verify(awsCredentialVerifier, times(0)).validateAws(credentialView, encodedAwsEnvPolicy);
    }

    @Test
    public void testVerifyIfRoleBasedCredentialVerificationShouldReturnVerifiedStatusWhenItIsCredentialCreationAndRoleIsNotAssumableWithoutExternalId()
            throws AwsPermissionMissingException, IOException {
        URL url = Resources.getResource("definitions/aws-environment-minimal-policy.json");
        String awsEnvPolicy = Resources.toString(url, UTF_8);
        String encodedAwsEnvPolicy = Base64Util.encode(awsEnvPolicy);
        String roleArn = "someRoleArn";
        when(awsPlatformParameters.getEnvironmentMinimalPoliciesJson()).thenReturn(Map.of(
                PolicyType.PUBLIC, encodedAwsEnvPolicy,
                PolicyType.GOV, encodedAwsEnvPolicy));
        when(credentialView.getRoleArn()).thenReturn(roleArn);
        SdkException amazonClientException = SdkException.builder().message(ROLE_IS_NOT_ASSUMABLE_ERROR_MESSAGE_INDICATOR).build();
        when(credentialClient.retrieveSessionCredentialsWithoutExternalId(any())).thenThrow(amazonClientException);
        CloudCredentialStatus result = underTest.verify(authenticatedContext, new CredentialVerificationContext(Boolean.TRUE));

        assertNotNull(result);
        assertEquals(CredentialStatus.VERIFIED, result.getStatus());
        assertNull(result.getException());

        verify(awsCredentialVerifier, times(1)).validateAws(any(), any());
        verify(awsCredentialVerifier, times(1)).validateAws(credentialView, encodedAwsEnvPolicy);
    }

    @Test
    public void testVerifyIfRoleBasedCredentialVerificationShouldReturnFailedStatusWhenItIsCredentialCreationAndRoleAssumeFailsWithoutExternalId()
            throws AwsPermissionMissingException, IOException {
        URL url = Resources.getResource("definitions/aws-environment-minimal-policy.json");
        String awsEnvPolicy = Resources.toString(url, UTF_8);
        String encodedAwsEnvPolicy = Base64Util.encode(awsEnvPolicy);
        String roleArn = "someRoleArn";
        when(credentialView.getRoleArn()).thenReturn(roleArn);
        SdkException amazonClientException = SdkException.builder().message("Something unexpected happened").build();
        when(credentialClient.retrieveSessionCredentialsWithoutExternalId(any())).thenThrow(amazonClientException);

        CloudCredentialStatus result = underTest.verify(authenticatedContext, new CredentialVerificationContext(Boolean.TRUE));

        assertNotNull(result);
        assertEquals(CredentialStatus.FAILED, result.getStatus());
        assertEquals(amazonClientException, result.getException());

        verify(awsCredentialVerifier, times(0)).validateAws(any(), any());
        verify(awsCredentialVerifier, times(0)).validateAws(credentialView, encodedAwsEnvPolicy);
    }

    @Test
    public void testVerifyByServiceIfRoleBasedCredentialVerificationThrowsSdkBaseExceptionThenFailed503StatusShouldReturn()
            throws IOException {

        URL url = Resources.getResource("definitions/aws-environment-minimal-policy.json");
        String awsEnvPolicy = Resources.toString(url, UTF_8);
        String encodedAwsEnvPolicy = Base64Util.encode(awsEnvPolicy);
        List<String> services = List.of("ml");
        Map<String, String> experiencePrerequisites = Map.of("ml", encodedAwsEnvPolicy);

        String roleArn = "someRoleArn";
        when(credentialView.getRoleArn()).thenReturn(roleArn);

        String exceptionMessageComesFromSdk = "SomethingTerribleHappened!";
        String expectedExceptionMessage = String.format("Unable to verify credential: check if the role '%s' exists and it's created with the correct " +
                "external ID. Cause: '%s'", roleArn, exceptionMessageComesFromSdk);
        Exception sdkException = new IllegalArgumentException(exceptionMessageComesFromSdk);

        when(credentialClient.retrieveSessionCredentials(any())).thenThrow(sdkException);
        CDPServicePolicyVerificationResponses result = underTest.verifyByServices(authenticatedContext, services, experiencePrerequisites);

        assertNotNull(result);
        assertEquals(1, result.getResults().size());
        assertEquals("ml", result.getResults().stream().findFirst().get().getServiceName());
        assertEquals(expectedExceptionMessage, result.getResults().stream().findFirst().get().getServiceStatus());
        assertEquals(503, result.getResults().stream().findFirst().get().getStatusCode());
    }

    @Test
    public void testVerifyByServiceIfRoleBasedCredentialVerificationThrowsAwsConfusedDeputyExceptionThenFailed503StatusShouldReturn()
            throws IOException {
        URL url = Resources.getResource("definitions/aws-environment-minimal-policy.json");
        String awsEnvPolicy = Resources.toString(url, UTF_8);
        String encodedAwsEnvPolicy = Base64Util.encode(awsEnvPolicy);
        List<String> services = List.of("ml");
        Map<String, String> experiencePrerequisites = Map.of("ml", encodedAwsEnvPolicy);

        String roleArn = "someRoleArn";
        when(credentialView.getRoleArn()).thenReturn(roleArn);
        Exception sdkException = new AwsConfusedDeputyException("SomethingTerribleHappened");
        when(credentialClient.retrieveSessionCredentials(any())).thenThrow(sdkException);

        CDPServicePolicyVerificationResponses result = underTest.verifyByServices(authenticatedContext, services, experiencePrerequisites);

        assertNotNull(result);
        assertEquals(1, result.getResults().size());
        assertEquals("ml", result.getResults().stream().findFirst().get().getServiceName());
        assertEquals("SomethingTerribleHappened", result.getResults().stream().findFirst().get().getServiceStatus());
        assertEquals(503, result.getResults().stream().findFirst().get().getStatusCode());
    }

    @Test
    public void testInternalAlternativeLookupShouldWorkFine() throws IOException {
        String awsEnvPolicy = Resources.toString(Resources.getResource("definitions/aws-environment-minimal-policy.json"), UTF_8);
        String encodedAwsEnvPolicy = Base64Util.encode(awsEnvPolicy);
        String service = MLX.getInternalAlternatives().stream().findFirst().get();
        List<String> services = List.of(service);
        Map<String, String> experiencePrerequisites = Map.of(MLX.getPublicName(), encodedAwsEnvPolicy);

        when(credentialView.getRoleArn()).thenReturn("someRoleArn");
        Exception sdkException = new AwsConfusedDeputyException("SomethingTerribleHappened");
        when(credentialClient.retrieveSessionCredentials(any())).thenThrow(sdkException);

        CDPServicePolicyVerificationResponses result = underTest.verifyByServices(authenticatedContext, services, experiencePrerequisites);

        assertNotNull(result);
        assertEquals(1, result.getResults().size());
        assertEquals(service, result.getResults().stream().findFirst().get().getServiceName());
        assertEquals("SomethingTerribleHappened", result.getResults().stream().findFirst().get().getServiceStatus());
        assertEquals(503, result.getResults().stream().findFirst().get().getStatusCode());
    }

    @Test
    public void testVerifyByServiceIfRoleBasedCredentialVerificationThrowsSdkBaseExceptionThenFailedStatusShouldReturn()
            throws IOException {

        URL url = Resources.getResource("definitions/aws-environment-minimal-policy.json");
        String awsEnvPolicy = Resources.toString(url, UTF_8);
        String encodedAwsEnvPolicy = Base64Util.encode(awsEnvPolicy);
        List<String> services = List.of("ml");
        Map<String, String> experiencePrerequisites = Map.of("ml", encodedAwsEnvPolicy);

        String roleArn = "someRoleArn";
        when(credentialView.getRoleArn()).thenReturn(roleArn);

        String exceptionMessageComesFromSdk = "SomethingTerribleHappened";
        Exception sdkException = new AwsConfusedDeputyException(exceptionMessageComesFromSdk);

        when(credentialClient.retrieveSessionCredentials(any())).thenThrow(sdkException);
        CDPServicePolicyVerificationResponses result = underTest.verifyByServices(authenticatedContext, services, experiencePrerequisites);

        assertNotNull(result);
        assertEquals(1, result.getResults().size());
        assertEquals("ml", result.getResults().stream().findFirst().get().getServiceName());
        assertEquals(exceptionMessageComesFromSdk, result.getResults().stream().findFirst().get().getServiceStatus());
        assertEquals(503, result.getResults().stream().findFirst().get().getStatusCode());
    }

    @Test
    public void testVerifyByServiceIfRoleBasedCredentialVerificationThrowsAmazonClientExceptionThenFailed503StatusShouldReturn()
            throws IOException {

        URL url = Resources.getResource("definitions/aws-environment-minimal-policy.json");
        String awsEnvPolicy = Resources.toString(url, UTF_8);
        String encodedAwsEnvPolicy = Base64Util.encode(awsEnvPolicy);
        List<String> services = List.of("ml");
        Map<String, String> experiencePrerequisites = Map.of("ml", encodedAwsEnvPolicy);

        String roleArn = "someRoleArn";
        when(credentialView.getRoleArn()).thenReturn(roleArn);

        String exceptionMessageComesFromSdk = "Unable to verify AWS credential due to: 'SomethingTerribleHappened'";
        Exception sdkException = SdkException.builder().message("SomethingTerribleHappened").build();
        when(credentialClient.retrieveSessionCredentials(any())).thenThrow(sdkException);
        CDPServicePolicyVerificationResponses result = underTest.verifyByServices(authenticatedContext, services, experiencePrerequisites);

        assertNotNull(result);
        assertEquals(1, result.getResults().size());
        assertEquals("ml", result.getResults().stream().findFirst().get().getServiceName());
        assertEquals(exceptionMessageComesFromSdk, result.getResults().stream().findFirst().get().getServiceStatus());
        assertEquals(503, result.getResults().stream().findFirst().get().getStatusCode());
    }

    @Test
    public void testVerifyByServiceIfKeyBasedCredentialAndRoleBasedNOTDefinedShouldThrowException()
            throws IOException {

        URL url = Resources.getResource("definitions/aws-environment-minimal-policy.json");
        String awsEnvPolicy = Resources.toString(url, UTF_8);
        String encodedAwsEnvPolicy = Base64Util.encode(awsEnvPolicy);
        List<String> services = List.of("ml");
        Map<String, String> experiencePrerequisites = Map.of("ml", encodedAwsEnvPolicy);

        when(credentialView.getAccessKey()).thenReturn(null);
        when(credentialView.getRoleArn()).thenReturn(null);
        when(credentialView.getSecretKey()).thenReturn(null);

        String exceptionMessageComesFromSdk = "Please provide both the 'access' and 'secret key'";

        CDPServicePolicyVerificationResponses result = underTest.verifyByServices(authenticatedContext, services, experiencePrerequisites);

        assertNotNull(result);
        assertEquals(1, result.getResults().size());
        assertEquals("ml", result.getResults().stream().findFirst().get().getServiceName());
        assertEquals(exceptionMessageComesFromSdk, result.getResults().stream().findFirst().get().getServiceStatus());
        assertEquals(503, result.getResults().stream().findFirst().get().getStatusCode());
    }

    @Test
    public void testVerifyByServiceIfOnlyKeyBasedCredentialWithAccessKeyAndRoleBasedNOTDefinedShouldThrowException()
            throws IOException {

        URL url = Resources.getResource("definitions/aws-environment-minimal-policy.json");
        String awsEnvPolicy = Resources.toString(url, UTF_8);
        String encodedAwsEnvPolicy = Base64Util.encode(awsEnvPolicy);
        List<String> services = List.of("ml");
        Map<String, String> experiencePrerequisites = Map.of("ml", encodedAwsEnvPolicy);

        String roleArn = "someRoleArn";
        when(credentialView.getAccessKey()).thenReturn(roleArn);
        when(credentialView.getRoleArn()).thenReturn(null);
        when(credentialView.getSecretKey()).thenReturn(null);

        String exceptionMessageComesFromSdk = "Please provide both the 'access' and 'secret key'";

        CDPServicePolicyVerificationResponses result = underTest.verifyByServices(authenticatedContext, services, experiencePrerequisites);

        assertNotNull(result);
        assertEquals(1, result.getResults().size());
        assertEquals("ml", result.getResults().stream().findFirst().get().getServiceName());
        assertEquals(exceptionMessageComesFromSdk, result.getResults().stream().findFirst().get().getServiceStatus());
        assertEquals(503, result.getResults().stream().findFirst().get().getStatusCode());
    }

    @Test
    public void testgetPrerequisites() throws IOException {

        Map<PolicyType, String> credentialPoliciesJson = Map.of(PolicyType.PUBLIC, "testCredentialPoliciesJson");
        Map<PolicyType, String> auditPoliciesJson = Map.of(PolicyType.PUBLIC, "testAuditPoliciesJson");
        Map<PolicyType, String> dynamodbJson = Map.of(PolicyType.PUBLIC, "testDynamodbJson");
        Map<PolicyType, String> bucketAccessJson = Map.of(PolicyType.PUBLIC, "testBucketAccessJson");
        Map<PolicyType, String> environmentJson = Map.of(PolicyType.PUBLIC, "testEnvironmentJson");
        Map<PolicyType, String> rangerAuditJson = Map.of(PolicyType.PUBLIC, "testRangerAuditJson");
        Map<PolicyType, String> rangeRazJson = Map.of(PolicyType.PUBLIC, "testRangeRazJson");
        Map<PolicyType, String> dataLakeAdminJson = Map.of(PolicyType.PUBLIC, "testDataLakeAdminJson");
        Map<PolicyType, String> logPolicyJson = Map.of(PolicyType.PUBLIC, "testlogPolicyJson");
        Map<PolicyType, String> datalakeBackupPolicyJson = Map.of(PolicyType.PUBLIC, "testdatalakeBackupPolicyJson");
        Map<PolicyType, String> datalakeRestorePolicyJson = Map.of(PolicyType.PUBLIC, "testdatalakeRestorePolicyJson");
        Map<PolicyType, String> idbrokerPolicyJson = Map.of(PolicyType.PUBLIC, "testIdbrokerPolicyJson");

        when(awsPlatformParameters.getCredentialPoliciesJson()).thenReturn(credentialPoliciesJson);
        when(awsPlatformParameters.getAuditPoliciesJson()).thenReturn(auditPoliciesJson);
        when(awsPlatformParameters.getCdpDynamoDbPolicyJson()).thenReturn(dynamodbJson);
        when(awsPlatformParameters.getCdpBucketAccessPolicyJson()).thenReturn(bucketAccessJson);
        when(awsPlatformParameters.getEnvironmentMinimalPoliciesJson()).thenReturn(environmentJson);
        when(awsPlatformParameters.getCdpRangerAuditS3PolicyJson()).thenReturn(rangerAuditJson);
        when(awsPlatformParameters.getCdpRangerRazS3PolicyJson()).thenReturn(rangeRazJson);
        when(awsPlatformParameters.getCdpDatalakeAdminS3PolicyJson()).thenReturn(dataLakeAdminJson);
        when(awsPlatformParameters.getCdpLogPolicyJson()).thenReturn(logPolicyJson);
        when(awsPlatformParameters.getCdpDatalakeBackupPolicyJson()).thenReturn(datalakeBackupPolicyJson);
        when(awsPlatformParameters.getCdpDatalakeRestorePolicyJson()).thenReturn(datalakeRestorePolicyJson);
        when(awsPlatformParameters.getCdpIdbrokerPolicyJson()).thenReturn(idbrokerPolicyJson);

        CredentialPrerequisitesResponse result = underTest.getPrerequisites(CloudContext.Builder.builder().build(), "externalId",
                "auditExtenralId", "deploymentaddress", CredentialType.ENVIRONMENT);

        assertNotNull(result);
        assertNotNull(result.getAws());
        assertEquals(11, result.getAws().getPolicies().size());
        assertTrue(result.getAws().getPolicies().containsKey("Audit"));
        assertTrue(result.getAws().getPolicies().containsKey("DynamoDB"));
        assertTrue(result.getAws().getPolicies().containsKey("Bucket_Access"));
        assertTrue(result.getAws().getPolicies().containsKey("Environment"));
        assertTrue(result.getAws().getPolicies().containsKey("Ranger_Audit"));
        assertTrue(result.getAws().getPolicies().containsKey("Ranger_Raz"));
        assertTrue(result.getAws().getPolicies().containsKey("Datalake_Admin"));
        assertTrue(result.getAws().getPolicies().containsKey("Datalake_Backup"));
        assertTrue(result.getAws().getPolicies().containsKey("Idbroker_Assumer"));
        assertTrue(result.getAws().getPolicies().containsKey("Datalake_Restore"));
        assertTrue(result.getAws().getPolicies().containsKey("Log_Policy"));

        assertEquals(10, result.getAws().getGranularPolicies().size());
        assertEquals(PRESENT_ONCE, result.getAws().getGranularPolicies().stream()
                .filter(policyResponse -> "Audit".equals(policyResponse.name())).count());
        assertEquals(PRESENT_ONCE, result.getAws().getGranularPolicies().stream()
                .filter(policyResponse -> "Bucket_Access".equals(policyResponse.name())).count());
        assertEquals(PRESENT_ONCE, result.getAws().getGranularPolicies().stream()
                .filter(policyResponse -> "Environment".equals(policyResponse.name())).count());
        assertEquals(PRESENT_ONCE, result.getAws().getGranularPolicies().stream()
                .filter(policyResponse -> "Ranger_Audit".equals(policyResponse.name())).count());
        assertEquals(PRESENT_ONCE, result.getAws().getGranularPolicies().stream()
                .filter(policyResponse -> "Ranger_Raz".equals(policyResponse.name())).count());
        assertEquals(PRESENT_ONCE, result.getAws().getGranularPolicies().stream()
                .filter(policyResponse -> "Datalake_Admin".equals(policyResponse.name())).count());
        assertEquals(PRESENT_ONCE, result.getAws().getGranularPolicies().stream()
                .filter(policyResponse -> "Datalake_Backup".equals(policyResponse.name())).count());
        assertEquals(PRESENT_ONCE, result.getAws().getGranularPolicies().stream()
                .filter(policyResponse -> "Idbroker_Assumer".equals(policyResponse.name())).count());
        assertEquals(PRESENT_ONCE, result.getAws().getGranularPolicies().stream()
                .filter(policyResponse -> "Datalake_Restore".equals(policyResponse.name())).count());
        assertEquals(PRESENT_ONCE, result.getAws().getGranularPolicies().stream()
                .filter(policyResponse -> "Log_Policy".equals(policyResponse.name())).count());
    }

}
