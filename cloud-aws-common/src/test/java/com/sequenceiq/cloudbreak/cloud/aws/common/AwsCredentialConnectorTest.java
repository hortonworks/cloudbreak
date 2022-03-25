package com.sequenceiq.cloudbreak.cloud.aws.common;

import static com.google.common.base.Charsets.UTF_8;
import static com.sequenceiq.cloudbreak.cloud.aws.common.AwsCredentialConnector.ROLE_IS_NOT_ASSUMABLE_ERROR_MESSAGE_INDICATOR;
import static com.sequenceiq.cloudbreak.experience.PolicyServiceName.MLX;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.URL;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.amazonaws.AmazonClientException;
import com.amazonaws.SdkBaseException;
import com.google.common.io.Resources;
import com.sequenceiq.cloudbreak.cloud.aws.common.exception.AwsConfusedDeputyException;
import com.sequenceiq.cloudbreak.cloud.aws.common.exception.AwsPermissionMissingException;
import com.sequenceiq.cloudbreak.cloud.aws.common.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.cloud.aws.common.view.AwsCredentialViewProvider;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CDPServicePolicyVerificationResponses;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredentialStatus;
import com.sequenceiq.cloudbreak.cloud.model.CredentialStatus;
import com.sequenceiq.cloudbreak.cloud.model.credential.CredentialVerificationContext;

public class AwsCredentialConnectorTest {
    private static final CredentialVerificationContext CREDENTIAL_VERIFICATION_CONTEXT = new CredentialVerificationContext(Boolean.FALSE);

    @Rule
    public final ExpectedException expectedException = ExpectedException.none();

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
    private AwsDefaultRegionSelector defaultRegionSelector;

    @InjectMocks
    private AwsCredentialConnector underTest;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(credential.isVerifyPermissions()).thenReturn(true);
        when(authenticatedContext.getCloudCredential()).thenReturn(credential);
        when(awsCredentialViewProvider.createAwsCredentialView(credential)).thenReturn(credentialView);
    }

    @Test
    public void testInteractiveLoginIsProhibitedOnAws() {
        expectedException.expect(UnsupportedOperationException.class);
        underTest.interactiveLogin(null, null, null);
    }

    @Test
    public void testVerifyIfRoleBasedCredentialVerificationThrowsSdkBaseExceptionThenFailedStatusShouldReturn()
        throws AwsPermissionMissingException, IOException {
        URL url = Resources.getResource("definitions/aws-environment-minimal-policy.json");
        String awsEnvPolicy = Resources.toString(url, UTF_8);
        String encodedAwsEnvPolicy = Base64.getEncoder().encodeToString(awsEnvPolicy.getBytes());
        String roleArn = "someRoleArn";
        when(credentialView.getRoleArn()).thenReturn(roleArn);

        String exceptionMessageComesFromSdk = "SomethingTerribleHappened!";
        String expectedExceptionMessage = String.format("Unable to verify credential: check if the role '%s' exists and it's created with the correct " +
                "external ID. Cause: '%s'", roleArn, exceptionMessageComesFromSdk);
        Exception sdkException = new SdkBaseException(exceptionMessageComesFromSdk);

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
        String encodedAwsEnvPolicy = Base64.getEncoder().encodeToString(awsEnvPolicy.getBytes());
        String roleArn = "someRoleArn";
        when(awsPlatformParameters.getEnvironmentMinimalPoliciesJson()).thenReturn(Map.of(
                PolicyType.PUBLIC, encodedAwsEnvPolicy,
                PolicyType.GOV, encodedAwsEnvPolicy));
        when(credentialView.getRoleArn()).thenReturn(roleArn);

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
        String encodedAwsEnvPolicy = Base64.getEncoder().encodeToString(awsEnvPolicy.getBytes());
        String roleArn = "someRoleArn";
        when(awsPlatformParameters.getEnvironmentMinimalPoliciesJson()).thenReturn(Map.of(
                PolicyType.PUBLIC, encodedAwsEnvPolicy,
                PolicyType.GOV, encodedAwsEnvPolicy));
        when(credentialView.getRoleArn()).thenReturn(roleArn);
        AmazonClientException amazonClientException = new AmazonClientException(ROLE_IS_NOT_ASSUMABLE_ERROR_MESSAGE_INDICATOR);
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
        String encodedAwsEnvPolicy = Base64.getEncoder().encodeToString(awsEnvPolicy.getBytes());
        String roleArn = "someRoleArn";
        when(credentialView.getRoleArn()).thenReturn(roleArn);
        AmazonClientException amazonClientException = new AmazonClientException("Something unexpected happened");
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
        String encodedAwsEnvPolicy = Base64.getEncoder().encodeToString(awsEnvPolicy.getBytes());
        List<String> services = List.of("ml");
        Map<String, String> experiencePrerequisites = Map.of("ml", encodedAwsEnvPolicy);

        String roleArn = "someRoleArn";
        when(credentialView.getRoleArn()).thenReturn(roleArn);

        String exceptionMessageComesFromSdk = "SomethingTerribleHappened!";
        String expectedExceptionMessage = String.format("Unable to verify credential: check if the role '%s' exists and it's created with the correct " +
                "external ID. Cause: '%s'", roleArn, exceptionMessageComesFromSdk);
        Exception sdkException = new SdkBaseException(exceptionMessageComesFromSdk);

        when(awsPlatformParameters.getEnvironmentMinimalPoliciesJson()).thenReturn(Map.of(
                PolicyType.PUBLIC, encodedAwsEnvPolicy,
                PolicyType.GOV, encodedAwsEnvPolicy));
        when(credentialClient.retrieveSessionCredentials(any())).thenThrow(sdkException);
        CDPServicePolicyVerificationResponses result = underTest.verifyByServices(authenticatedContext, services, experiencePrerequisites);

        assertNotNull(result);
        assertEquals(result.getResults().size(), 1);
        assertEquals(result.getResults().stream().findFirst().get().getServiceName(), "ml");
        assertEquals(result.getResults().stream().findFirst().get().getServiceStatus(), expectedExceptionMessage);
        assertEquals(result.getResults().stream().findFirst().get().getStatusCode(), 503);
    }

    @Test
    public void testVerifyByServiceIfRoleBasedCredentialVerificationThrowsAwsConfusedDeputyExceptionThenFailed503StatusShouldReturn()
            throws IOException {
        URL url = Resources.getResource("definitions/aws-environment-minimal-policy.json");
        String awsEnvPolicy = Resources.toString(url, UTF_8);
        String encodedAwsEnvPolicy = Base64.getEncoder().encodeToString(awsEnvPolicy.getBytes());
        List<String> services = List.of("ml");
        Map<String, String> experiencePrerequisites = Map.of("ml", encodedAwsEnvPolicy);

        String roleArn = "someRoleArn";
        when(credentialView.getRoleArn()).thenReturn(roleArn);

        String exceptionMessageComesFromSdk = "Unable to verify credential: check if the role 'someRoleArn' exists " +
                "and it's created with the correct external ID. Cause: 'SomethingTerribleHappened!!";
        Exception sdkException = new AwsConfusedDeputyException("SomethingTerribleHappened");

        when(awsPlatformParameters.getEnvironmentMinimalPoliciesJson()).thenReturn(Map.of(
                PolicyType.PUBLIC, encodedAwsEnvPolicy,
                PolicyType.GOV, encodedAwsEnvPolicy));
        when(credentialClient.retrieveSessionCredentials(any())).thenThrow(sdkException);
        CDPServicePolicyVerificationResponses result = underTest.verifyByServices(authenticatedContext, services, experiencePrerequisites);

        assertNotNull(result);
        assertEquals(result.getResults().size(), 1);
        assertEquals(result.getResults().stream().findFirst().get().getServiceName(), "ml");
        assertEquals(result.getResults().stream().findFirst().get().getServiceStatus(), "SomethingTerribleHappened");
        assertEquals(result.getResults().stream().findFirst().get().getStatusCode(), 503);
    }

    @Test
    public void testInternalAlternativeLookupShouldWorkFine() throws IOException {
        String awsEnvPolicy = Resources.toString(Resources.getResource("definitions/aws-environment-minimal-policy.json"), UTF_8);
        String encodedAwsEnvPolicy = Base64.getEncoder().encodeToString(awsEnvPolicy.getBytes());
        String service = MLX.getInternalAlternatives().stream().findFirst().get();
        List<String> services = List.of(service);
        Map<String, String> experiencePrerequisites = Map.of(MLX.getPublicName(), encodedAwsEnvPolicy);

        when(credentialView.getRoleArn()).thenReturn("someRoleArn");
        Exception sdkException = new AwsConfusedDeputyException("SomethingTerribleHappened");

        when(awsPlatformParameters.getEnvironmentMinimalPoliciesJson()).thenReturn(Map.of(
                PolicyType.PUBLIC, encodedAwsEnvPolicy,
                PolicyType.GOV, encodedAwsEnvPolicy));
        when(credentialClient.retrieveSessionCredentials(any())).thenThrow(sdkException);

        CDPServicePolicyVerificationResponses result = underTest.verifyByServices(authenticatedContext, services, experiencePrerequisites);

        assertNotNull(result);
        assertEquals(result.getResults().size(), 1);
        assertEquals(result.getResults().stream().findFirst().get().getServiceName(), service);
        assertEquals(result.getResults().stream().findFirst().get().getServiceStatus(), "SomethingTerribleHappened");
        assertEquals(result.getResults().stream().findFirst().get().getStatusCode(), 503);
    }

    @Test
    public void testVerifyByServiceIfRoleBasedCredentialVerificationThrowsSdkBaseExceptionThenFailedStatusShouldReturn()
            throws IOException {

        URL url = Resources.getResource("definitions/aws-environment-minimal-policy.json");
        String awsEnvPolicy = Resources.toString(url, UTF_8);
        String encodedAwsEnvPolicy = Base64.getEncoder().encodeToString(awsEnvPolicy.getBytes());
        List<String> services = List.of("ml");
        Map<String, String> experiencePrerequisites = Map.of("ml", encodedAwsEnvPolicy);

        String roleArn = "someRoleArn";
        when(credentialView.getRoleArn()).thenReturn(roleArn);

        String exceptionMessageComesFromSdk = "Unable to verify credential: check if the role 'someRoleArn' exists " +
                "and it's created with the correct external ID. Cause: 'SomethingTerribleHappened!!";
        Exception sdkException = new AwsConfusedDeputyException("SomethingTerribleHappened");

        when(awsPlatformParameters.getEnvironmentMinimalPoliciesJson()).thenReturn(Map.of(
                PolicyType.PUBLIC, encodedAwsEnvPolicy,
                PolicyType.GOV, encodedAwsEnvPolicy));
        when(credentialClient.retrieveSessionCredentials(any())).thenThrow(sdkException);
        CDPServicePolicyVerificationResponses result = underTest.verifyByServices(authenticatedContext, services, experiencePrerequisites);

        assertNotNull(result);
        assertEquals(result.getResults().size(), 1);
        assertEquals(result.getResults().stream().findFirst().get().getServiceName(), "ml");
        assertEquals(result.getResults().stream().findFirst().get().getServiceStatus(), "SomethingTerribleHappened");
        assertEquals(result.getResults().stream().findFirst().get().getStatusCode(), 503);
    }

    @Test
    public void testVerifyByServiceIfRoleBasedCredentialVerificationThrowsAmazonClientExceptionThenFailed503StatusShouldReturn()
            throws IOException {

        URL url = Resources.getResource("definitions/aws-environment-minimal-policy.json");
        String awsEnvPolicy = Resources.toString(url, UTF_8);
        String encodedAwsEnvPolicy = Base64.getEncoder().encodeToString(awsEnvPolicy.getBytes());
        List<String> services = List.of("ml");
        Map<String, String> experiencePrerequisites = Map.of("ml", encodedAwsEnvPolicy);

        String roleArn = "someRoleArn";
        when(credentialView.getRoleArn()).thenReturn(roleArn);

        String exceptionMessageComesFromSdk = "Unable to verify AWS credential due to: 'SomethingTerribleHappened'";
        Exception sdkException = new AmazonClientException("SomethingTerribleHappened");

        when(awsPlatformParameters.getEnvironmentMinimalPoliciesJson()).thenReturn(Map.of(
                PolicyType.PUBLIC, encodedAwsEnvPolicy,
                PolicyType.GOV, encodedAwsEnvPolicy));
        when(credentialClient.retrieveSessionCredentials(any())).thenThrow(sdkException);
        CDPServicePolicyVerificationResponses result = underTest.verifyByServices(authenticatedContext, services, experiencePrerequisites);

        assertNotNull(result);
        assertEquals(result.getResults().size(), 1);
        assertEquals(result.getResults().stream().findFirst().get().getServiceName(), "ml");
        assertEquals(result.getResults().stream().findFirst().get().getServiceStatus(), exceptionMessageComesFromSdk);
        assertEquals(result.getResults().stream().findFirst().get().getStatusCode(), 503);
    }

    @Test
    public void testVerifyByServiceIfKeyBasedCredentialAndRoleBasedNOTDefinedShouldThrowException()
            throws IOException {

        URL url = Resources.getResource("definitions/aws-environment-minimal-policy.json");
        String awsEnvPolicy = Resources.toString(url, UTF_8);
        String encodedAwsEnvPolicy = Base64.getEncoder().encodeToString(awsEnvPolicy.getBytes());
        List<String> services = List.of("ml");
        Map<String, String> experiencePrerequisites = Map.of("ml", encodedAwsEnvPolicy);

        String roleArn = null;
        when(credentialView.getAccessKey()).thenReturn(roleArn);
        when(credentialView.getRoleArn()).thenReturn(roleArn);
        when(credentialView.getSecretKey()).thenReturn(roleArn);

        String exceptionMessageComesFromSdk = "Please provide both the 'access' and 'secret key'";

        when(awsPlatformParameters.getEnvironmentMinimalPoliciesJson()).thenReturn(Map.of(
                PolicyType.PUBLIC, encodedAwsEnvPolicy,
                PolicyType.GOV, encodedAwsEnvPolicy));
        CDPServicePolicyVerificationResponses result = underTest.verifyByServices(authenticatedContext, services, experiencePrerequisites);

        assertNotNull(result);
        assertEquals(result.getResults().size(), 1);
        assertEquals(result.getResults().stream().findFirst().get().getServiceName(), "ml");
        assertEquals(result.getResults().stream().findFirst().get().getServiceStatus(), exceptionMessageComesFromSdk);
        assertEquals(result.getResults().stream().findFirst().get().getStatusCode(), 503);
    }

    @Test
    public void testVerifyByServiceIfOnlyKeyBasedCredentialWithAccessKeyAndRoleBasedNOTDefinedShouldThrowException()
        throws IOException {

        URL url = Resources.getResource("definitions/aws-environment-minimal-policy.json");
        String awsEnvPolicy = Resources.toString(url, UTF_8);
        String encodedAwsEnvPolicy = Base64.getEncoder().encodeToString(awsEnvPolicy.getBytes());
        List<String> services = List.of("ml");
        Map<String, String> experiencePrerequisites = Map.of("ml", encodedAwsEnvPolicy);

        String roleArn = "someRoleArn";
        when(credentialView.getAccessKey()).thenReturn(roleArn);
        when(credentialView.getRoleArn()).thenReturn(null);
        when(credentialView.getSecretKey()).thenReturn(null);

        String exceptionMessageComesFromSdk = "Please provide both the 'access' and 'secret key'";

        when(awsPlatformParameters.getEnvironmentMinimalPoliciesJson()).thenReturn(Map.of(
                PolicyType.PUBLIC, encodedAwsEnvPolicy,
                PolicyType.GOV, encodedAwsEnvPolicy)
        );
        CDPServicePolicyVerificationResponses result = underTest.verifyByServices(authenticatedContext, services, experiencePrerequisites);

        assertNotNull(result);
        assertEquals(result.getResults().size(), 1);
        assertEquals(result.getResults().stream().findFirst().get().getServiceName(), "ml");
        assertEquals(result.getResults().stream().findFirst().get().getServiceStatus(), exceptionMessageComesFromSdk);
        assertEquals(result.getResults().stream().findFirst().get().getStatusCode(), 503);
    }

}
