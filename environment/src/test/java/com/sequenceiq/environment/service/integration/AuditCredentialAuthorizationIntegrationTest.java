package com.sequenceiq.environment.service.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.ws.rs.ForbiddenException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.auth.altus.GrpcUmsClient;
import com.sequenceiq.cloudbreak.auth.crn.CrnTestUtil;
import com.sequenceiq.cloudbreak.cloud.event.platform.ResourceDefinitionRequest;
import com.sequenceiq.cloudbreak.cloud.event.platform.ResourceDefinitionResult;
import com.sequenceiq.cloudbreak.quartz.configuration.QuartzJobInitializer;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;
import com.sequenceiq.common.model.CredentialType;
import com.sequenceiq.environment.api.v1.credential.model.parameters.aws.AwsCredentialParameters;
import com.sequenceiq.environment.api.v1.credential.model.parameters.aws.KeyBasedParameters;
import com.sequenceiq.environment.api.v1.credential.model.request.CredentialRequest;
import com.sequenceiq.environment.client.EnvironmentServiceClientBuilder;
import com.sequenceiq.environment.client.EnvironmentServiceCrnEndpoints;
import com.sequenceiq.environment.credential.domain.Credential;
import com.sequenceiq.environment.credential.repository.CredentialRepository;
import com.sequenceiq.environment.credential.service.RequestProvider;
import com.sequenceiq.environment.service.integration.testconfiguration.TestConfigurationForServiceIntegration;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = TestConfigurationForServiceIntegration.class, properties = "info.app.version=test")
@ActiveProfiles("test")
public class AuditCredentialAuthorizationIntegrationTest {

    private static final String DEFINITION_AWS = FileReaderUtils.readFileFromClasspathQuietly("testCredentialDefinitionAws.json");

    private static final String ACCOUNT_ID = "acc";

    private static final String FIRST_CRED_NAME = "cred1";

    private static final String SECOND_CRED_NAME = "cred2";

    private static final String FIRST_USER_ID = "1";

    private static final String SECOND_USER_ID = "2";

    private static final String FIRST_USER_CRN  = getUserCrn(FIRST_USER_ID, ACCOUNT_ID);

    private static final String SECOND_USER_CRN = getUserCrn(SECOND_USER_ID, ACCOUNT_ID);

    @LocalServerPort
    private int port;

    @MockBean
    private GrpcUmsClient grpcUmsClient;

    @MockBean
    private RequestProvider requestProvider;

    @MockBean
    private EntitlementService entitlementService;

    @MockBean
    private QuartzJobInitializer quartzJobInitializer;

    @Mock
    private ResourceDefinitionRequest resourceDefinitionRequest;

    @Inject
    private CredentialRepository credentialRepository;

    private EnvironmentServiceCrnEndpoints firstUserClient;

    private EnvironmentServiceCrnEndpoints secondUserClient;

    @BeforeEach
    public void setup() {
        firstUserClient = getClient(FIRST_USER_CRN);
        secondUserClient = getClient(SECOND_USER_CRN);
        doNothing().when(grpcUmsClient).assignResourceRole(anyString(), anyString(), anyString(), any());
        mockPermissions();
        when(grpcUmsClient.getResourceRoles(any())).thenReturn(Set.of(
                "crn:altus:iam:us-west-1:altus:resourceRole:Owner",
                "crn:altus:iam:us-west-1:altus:resourceRole:EnvironmentAdmin"));
    }

    @AfterEach
    public void cleanUpDb() {
        credentialRepository.deleteAll();
    }

    @Test
    public void testAuditCredentialCreateAws() throws InterruptedException {
        when(requestProvider.getResourceDefinitionRequest(any(), any())).thenReturn(resourceDefinitionRequest);
        when(requestProvider.getCredentialVerificationRequest(any(), any(), anyBoolean())).thenAnswer(
                invocation -> new EnvironmentServiceIntegrationTest.CredentialVerificationMockRequest(invocation.getArgument(0), invocation.getArgument(1))
        );
        when(resourceDefinitionRequest.await()).thenReturn(new ResourceDefinitionResult(1L, DEFINITION_AWS));

        assertNotNull(firstUserClient.auditCredentialV1Endpoint().post(getAwsCredentialRequest(FIRST_CRED_NAME)));
        assertThrows(ForbiddenException.class, () ->
                secondUserClient.auditCredentialV1Endpoint().post(getAwsCredentialRequest(SECOND_CRED_NAME)));
    }

    @Test
    public void testAuditCredentialPermissions() {
        credentialRepository.save(getAwsCredential(FIRST_CRED_NAME, ACCOUNT_ID, FIRST_USER_CRN));

        testHappyPaths(firstUserClient, FIRST_CRED_NAME);

        testUnhappyPaths(secondUserClient, FIRST_CRED_NAME);

        assertEquals(1, credentialRepository.findAll().stream()
                .filter(cred -> !cred.isArchived()).collect(Collectors.toList()).size());
    }

    private void testHappyPaths(EnvironmentServiceCrnEndpoints client, String credentialName) {
        String resourceCrn = getResourceCrn(credentialName, ACCOUNT_ID);

        client.auditCredentialV1Endpoint().list();
        client.auditCredentialV1Endpoint().getByResourceCrn(resourceCrn);
    }

    private void testUnhappyPaths(EnvironmentServiceCrnEndpoints client, String wrongCredentialName) {
        String wrongCredentialCrn = getResourceCrn(wrongCredentialName, ACCOUNT_ID);

        assertThrows(ForbiddenException.class, () -> client.auditCredentialV1Endpoint().list());
        assertThrows(ForbiddenException.class, () -> client.auditCredentialV1Endpoint().getByResourceCrn(wrongCredentialCrn));
    }

    private static String getUserCrn(String userId, String accountId) {
        return CrnTestUtil.getUserCrnBuilder()
                .setAccountId(accountId)
                .setResource(userId)
                .build()
                .toString();
    }

    private static String getResourceCrn(String credentialName, String accountId) {
        return CrnTestUtil.getCredentialCrnBuilder()
                .setAccountId(accountId)
                .setResource(credentialName)
                .build()
                .toString();
    }

    private void mockPermissions() {
        when(grpcUmsClient.checkAccountRight(eq(FIRST_USER_CRN), anyString(), any())).thenReturn(Boolean.TRUE);
        when(grpcUmsClient.checkAccountRight(eq(SECOND_USER_CRN), anyString(), any())).thenReturn(Boolean.FALSE);
    }

    private CredentialRequest getAwsCredentialRequest(String name) {
        CredentialRequest credentialRequest = new CredentialRequest();
        credentialRequest.setAws(getAwsKeyBasedCredentialParameters());
        credentialRequest.setCloudPlatform("AWS");
        credentialRequest.setName(name);
        return credentialRequest;
    }

    private AwsCredentialParameters getAwsKeyBasedCredentialParameters() {
        AwsCredentialParameters aws = new AwsCredentialParameters();
        aws.setGovCloud(false);
        KeyBasedParameters keyBased = new KeyBasedParameters();
        keyBased.setAccessKey("accessKey");
        keyBased.setSecretKey("secretKey");
        aws.setKeyBased(keyBased);
        return aws;
    }

    private Credential getAwsCredential(String name, String accountId, String userCrn) {
        Credential credential = new Credential();
        credential.setName(name);
        credential.setResourceCrn(getResourceCrn(name, accountId));
        credential.setAccountId(accountId);
        credential.setCloudPlatform("AWS");
        credential.setCreator(userCrn);
        credential.setDescription("description");
        credential.setType(CredentialType.AUDIT);
        credential.setGovCloud(false);
        credential.setArchived(false);
        return credential;
    }

    private EnvironmentServiceCrnEndpoints getClient(String userCrn) {
        return new EnvironmentServiceClientBuilder(String.format("http://localhost:%d/environmentservice", port))
                .withCertificateValidation(false)
                .withDebug(true)
                .withIgnorePreValidation(true)
                .build()
                .withCrn(userCrn);
    }
}
