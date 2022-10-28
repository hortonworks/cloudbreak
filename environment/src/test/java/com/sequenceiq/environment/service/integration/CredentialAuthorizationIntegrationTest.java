package com.sequenceiq.environment.service.integration;

import static com.sequenceiq.authorization.resource.AuthorizationResourceAction.DELETE_CREDENTIAL;
import static com.sequenceiq.authorization.resource.AuthorizationResourceAction.DESCRIBE_CREDENTIAL;
import static com.sequenceiq.authorization.resource.AuthorizationResourceAction.EDIT_CREDENTIAL;
import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
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

import com.cloudera.thunderhead.service.authorization.AuthorizationProto;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
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
import com.sequenceiq.environment.api.v1.credential.model.request.EditCredentialRequest;
import com.sequenceiq.environment.client.EnvironmentServiceClientBuilder;
import com.sequenceiq.environment.client.EnvironmentServiceCrnEndpoints;
import com.sequenceiq.environment.credential.domain.Credential;
import com.sequenceiq.environment.credential.repository.CredentialRepository;
import com.sequenceiq.environment.credential.service.RequestProvider;
import com.sequenceiq.environment.service.integration.testconfiguration.TestConfigurationForServiceIntegration;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = TestConfigurationForServiceIntegration.class,
        properties = "info.app.version=test")
@ActiveProfiles("test")
public class CredentialAuthorizationIntegrationTest {

    private static final String DEFINITION_AWS = FileReaderUtils.readFileFromClasspathQuietly("testCredentialDefinitionAws.json");

    private static final String ACCOUNT_ID = "acc";

    private static final String FIRST_CRED_NAME = "cred1";

    private static final String SECOND_CRED_NAME = "cred2";

    private static final String FIRST_USER_ID = "1";

    private static final String SECOND_USER_ID = "2";

    private static final String FIRST_USER_CRN = getUserCrn(FIRST_USER_ID, ACCOUNT_ID);

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
        lenient().when(grpcUmsClient.hasRights(anyString(), anyList(), any(), any())).then(i -> {
            List<AuthorizationProto.RightCheck> rightChecks = i.getArgument(2);
            return rightChecks.stream().map(r -> Boolean.TRUE).collect(toList());
        });
        lenient().when(grpcUmsClient.checkAccountRight(anyString(), anyString(), any())).thenReturn(true);
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
    public void testCredentialCreateAws() throws InterruptedException {
        when(requestProvider.getResourceDefinitionRequest(any(), any())).thenReturn(resourceDefinitionRequest);
        when(requestProvider.getCredentialVerificationRequest(any(), any(), anyBoolean())).thenAnswer(
                invocation -> new EnvironmentServiceIntegrationTest.CredentialVerificationMockRequest(invocation.getArgument(0), invocation.getArgument(1))
        );
        when(resourceDefinitionRequest.await()).thenReturn(new ResourceDefinitionResult(1L, DEFINITION_AWS));

        when(grpcUmsClient.checkAccountRight(eq(FIRST_USER_CRN), anyString(), any())).thenReturn(Boolean.TRUE);
        when(grpcUmsClient.checkAccountRight(eq(SECOND_USER_CRN), anyString(), any())).thenReturn(Boolean.FALSE);

        assertNotNull(firstUserClient.credentialV1Endpoint().post(getAwsCredentialRequest(FIRST_CRED_NAME)));
        assertThrows(ForbiddenException.class, () ->
                secondUserClient.credentialV1Endpoint().post(getAwsCredentialRequest(SECOND_CRED_NAME)));
    }

    @Test
    public void testCredentialPermissions() {
        credentialRepository.save(getAwsCredential(FIRST_CRED_NAME, ACCOUNT_ID, FIRST_USER_CRN));
        credentialRepository.save(getAwsCredential(SECOND_CRED_NAME, ACCOUNT_ID, SECOND_USER_CRN));

        testUnhappyPaths(firstUserClient, SECOND_CRED_NAME);
        testUnhappyPaths(secondUserClient, FIRST_CRED_NAME);

        testHappyPaths(firstUserClient, FIRST_CRED_NAME);
        testHappyPaths(secondUserClient, SECOND_CRED_NAME);

        assertEquals(0, credentialRepository.findAll().stream()
                .filter(cred -> !cred.isArchived()).collect(Collectors.toList()).size());
    }

    private void testHappyPaths(EnvironmentServiceCrnEndpoints client, String credentialName) {
        client.credentialV1Endpoint().getByName(credentialName);
        client.credentialV1Endpoint().deleteByName(credentialName);
    }

    private void testUnhappyPaths(EnvironmentServiceCrnEndpoints client, String wrongCredentialName) {
        String wrongCredentialCrn = getResourceCrn(wrongCredentialName, ACCOUNT_ID);

        assertThrows(ForbiddenException.class, () -> client.credentialV1Endpoint().getByName(wrongCredentialName));
        assertThrows(ForbiddenException.class, () -> client.credentialV1Endpoint().getByResourceCrn(wrongCredentialCrn));
        assertThrows(ForbiddenException.class, () -> client.credentialV1Endpoint().put(getAwsEditCredentialRequest(wrongCredentialName)));
        assertThrows(ForbiddenException.class, () -> client.credentialV1Endpoint().deleteMultiple(Sets.newHashSet(wrongCredentialName)));
        assertThrows(ForbiddenException.class, () -> client.credentialV1Endpoint().deleteByName(wrongCredentialName));
        assertThrows(ForbiddenException.class, () -> client.credentialV1Endpoint().deleteByResourceCrn(wrongCredentialCrn));
        assertThrows(ForbiddenException.class, () -> client.credentialV1Endpoint().verifyByName(wrongCredentialName));
        assertThrows(ForbiddenException.class, () -> client.credentialV1Endpoint().verifyByCrn(wrongCredentialCrn));
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
        String firstCredentialCrn = getResourceCrn(FIRST_CRED_NAME, ACCOUNT_ID);
        String secondCredentialCrn = getResourceCrn(SECOND_CRED_NAME, ACCOUNT_ID);

        List<Boolean> firstUserResult = Lists.newArrayList();
        firstUserResult.add(Boolean.TRUE);
        firstUserResult.add(Boolean.FALSE);
        when(grpcUmsClient.hasRights(eq(FIRST_USER_CRN), anyList(), any())).thenReturn(firstUserResult);
        List<Boolean> secondUserResult = Lists.newArrayList();
        secondUserResult.add(Boolean.TRUE);
        secondUserResult.add(Boolean.FALSE);
        when(grpcUmsClient.hasRights(eq(SECOND_USER_CRN), anyList(), any())).thenReturn(secondUserResult);

        AuthorizationProto.RightCheck firstCredDescribeCheck = AuthorizationProto.RightCheck.newBuilder()
                .setResource(firstCredentialCrn)
                .setRight(DESCRIBE_CREDENTIAL.getRight())
                .build();
        when(grpcUmsClient.hasRights(eq(FIRST_USER_CRN), eq(List.of(firstCredDescribeCheck)), any()))
                .thenReturn(List.of(Boolean.TRUE));
        when(grpcUmsClient.hasRights(eq(SECOND_USER_CRN), eq(List.of(firstCredDescribeCheck)), any()))
                .thenReturn(List.of(Boolean.FALSE));

        AuthorizationProto.RightCheck secondCredDescribeCheck = AuthorizationProto.RightCheck.newBuilder()
                .setResource(secondCredentialCrn)
                .setRight(DESCRIBE_CREDENTIAL.getRight())
                .build();
        when(grpcUmsClient.hasRights(eq(FIRST_USER_CRN), eq(List.of(secondCredDescribeCheck)), any()))
                .thenReturn(List.of(Boolean.FALSE));
        when(grpcUmsClient.hasRights(eq(SECOND_USER_CRN), eq(List.of(secondCredDescribeCheck)), any()))
                .thenReturn(List.of(Boolean.TRUE));

        AuthorizationProto.RightCheck firstCredEditCheck = AuthorizationProto.RightCheck.newBuilder()
                .setResource(firstCredentialCrn)
                .setRight(EDIT_CREDENTIAL.getRight())
                .build();
        when(grpcUmsClient.hasRights(eq(FIRST_USER_CRN), eq(List.of(firstCredEditCheck)), any())).thenReturn(List.of(Boolean.TRUE));
        when(grpcUmsClient.hasRights(eq(SECOND_USER_CRN), eq(List.of(firstCredEditCheck)), any())).thenReturn(List.of(Boolean.FALSE));

        AuthorizationProto.RightCheck secondCredEditCheck = AuthorizationProto.RightCheck.newBuilder()
                .setResource(secondCredentialCrn)
                .setRight(EDIT_CREDENTIAL.getRight())
                .build();
        when(grpcUmsClient.hasRights(eq(FIRST_USER_CRN), eq(List.of(secondCredEditCheck)), any())).thenReturn(List.of(Boolean.FALSE));
        when(grpcUmsClient.hasRights(eq(SECOND_USER_CRN), eq(List.of(secondCredEditCheck)), any())).thenReturn(List.of(Boolean.FALSE));

        AuthorizationProto.RightCheck firstCredDeleteCheck = AuthorizationProto.RightCheck.newBuilder()
                .setResource(firstCredentialCrn)
                .setRight(DELETE_CREDENTIAL.getRight())
                .build();
        when(grpcUmsClient.hasRights(eq(FIRST_USER_CRN), eq(List.of(firstCredDeleteCheck)), any())).thenReturn(List.of(Boolean.TRUE));
        when(grpcUmsClient.hasRights(eq(SECOND_USER_CRN), eq(List.of(firstCredDeleteCheck)), any())).thenReturn(List.of(Boolean.FALSE));

        AuthorizationProto.RightCheck secondCredDeleteCheck = AuthorizationProto.RightCheck.newBuilder()
                .setResource(secondCredentialCrn)
                .setRight(DELETE_CREDENTIAL.getRight())
                .build();
        when(grpcUmsClient.hasRights(eq(FIRST_USER_CRN), eq(List.of(secondCredDeleteCheck)), any())).thenReturn(List.of(Boolean.FALSE));
        when(grpcUmsClient.hasRights(eq(SECOND_USER_CRN), eq(List.of(secondCredDeleteCheck)), any())).thenReturn(List.of(Boolean.TRUE));

        when(grpcUmsClient.hasRights(eq(FIRST_USER_CRN), eq(List.of(secondCredentialCrn)),
                eq(DELETE_CREDENTIAL.getRight()), any())).thenReturn(Map.of(secondCredentialCrn, Boolean.FALSE));
        when(grpcUmsClient.hasRights(eq(SECOND_USER_CRN), eq(List.of(secondCredentialCrn)),
                eq(DELETE_CREDENTIAL.getRight()), any())).thenReturn(Map.of(secondCredentialCrn, Boolean.TRUE));

        when(grpcUmsClient.hasRights(eq(FIRST_USER_CRN), eq(List.of(firstCredentialCrn)),
                eq(DELETE_CREDENTIAL.getRight()), any())).thenReturn(Map.of(secondCredentialCrn, Boolean.TRUE));
        when(grpcUmsClient.hasRights(eq(SECOND_USER_CRN), eq(List.of(firstCredentialCrn)),
                eq(DELETE_CREDENTIAL.getRight()), any())).thenReturn(Map.of(secondCredentialCrn, Boolean.FALSE));
    }

    private CredentialRequest getAwsCredentialRequest(String name) {
        CredentialRequest credentialRequest = new CredentialRequest();
        credentialRequest.setAws(getAwsKeyBasedCredentialParameters());
        credentialRequest.setCloudPlatform("AWS");
        credentialRequest.setName(name);
        return credentialRequest;
    }

    private EditCredentialRequest getAwsEditCredentialRequest(String name) {
        EditCredentialRequest credentialRequest = new EditCredentialRequest();
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
        credential.setType(CredentialType.ENVIRONMENT);
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
