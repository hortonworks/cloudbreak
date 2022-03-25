package com.sequenceiq.environment.credential.service;

import static com.sequenceiq.common.model.CredentialType.AUDIT;
import static com.sequenceiq.common.model.CredentialType.ENVIRONMENT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.AdditionalMatchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import javax.inject.Inject;
import javax.ws.rs.BadRequestException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.sequenceiq.authorization.service.OwnerAssignmentService;
import com.sequenceiq.cloudbreak.auth.altus.GrpcUmsClient;
import com.sequenceiq.cloudbreak.auth.crn.CrnTestUtil;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareCrnGenerator;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.message.CloudbreakMessagesService;
import com.sequenceiq.cloudbreak.service.secret.service.SecretService;
import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.common.model.CredentialType;
import com.sequenceiq.environment.credential.attributes.CredentialAttributes;
import com.sequenceiq.environment.credential.attributes.azure.AzureCredentialAttributes;
import com.sequenceiq.environment.credential.attributes.azure.CodeGrantFlowAttributes;
import com.sequenceiq.environment.credential.domain.Credential;
import com.sequenceiq.environment.credential.exception.CredentialOperationException;
import com.sequenceiq.environment.credential.repository.CredentialRepository;
import com.sequenceiq.environment.credential.validation.CredentialValidator;
import com.sequenceiq.environment.credential.verification.CredentialVerification;
import com.sequenceiq.environment.environment.verification.PolicyValidationErrorResponseConverter;
import com.sequenceiq.notification.NotificationSender;

@SpringBootTest
@TestPropertySource(properties = "environment.enabledplatforms=AWS, AZURE, BLAH, BAZ")
class CredentialServiceTest {

    private static final Credential CREDENTIAL = new Credential();

    private static final String PLATFORM = "PLATFORM";

    private static final String DIFFERENT_CODE = "1234";

    private static final String CODE = "6789";

    private static final String STATE = "state";

    private static final String DIFFERENT_STATE = "different state";

    private static final String CREDENTIAL_NAME = "test";

    private static final String REDIRECT_URL = "http://cloudera.com";

    private static final String DEPLOYMENT_ADDRESS = "address";

    private static final String USER_ID = "TEST";

    private static final String ACCOUNT_ID = "123";

    private static final String AWS = "AWS";

    private static final String AZURE = "AZURE";

    private static final String BLAH = "BLAH";

    private static final String BAZ = "BAZ";

    @Inject
    private CredentialService credentialServiceUnderTest;

    @MockBean
    private CredentialRepository repository;

    @MockBean
    private CredentialValidator credentialValidator;

    @MockBean
    private ServiceProviderCredentialAdapter credentialAdapter;

    @MockBean
    private CredentialPrerequisiteService credentialPrerequisiteService;

    @MockBean
    private SecretService secretService;

    @MockBean
    private NotificationSender notificationSender;

    @MockBean
    private CloudbreakMessagesService cloudbreakMessagesService;

    @MockBean
    private GrpcUmsClient grpcUmsClient;

    @MockBean
    private OwnerAssignmentService ownerAssignmentService;

    @MockBean
    private TransactionService transactionService;

    @MockBean
    private RegionAwareCrnGenerator regionAwareCrnGenerator;

    @MockBean
    private PolicyValidationErrorResponseConverter policyValidationErrorResponseConverter;

    @BeforeEach
    void setupTestCredential() throws TransactionService.TransactionExecutionException {
        CrnTestUtil.mockCrnGenerator(regionAwareCrnGenerator);

        CREDENTIAL.setName(CREDENTIAL_NAME);
        CREDENTIAL.setCloudPlatform(PLATFORM);
        String credentialAttributesSecret = getTestAttributes(STATE, DEPLOYMENT_ADDRESS, REDIRECT_URL);
        CREDENTIAL.setAttributes(credentialAttributesSecret);

        lenient().doAnswer(invocation -> ((Supplier<?>) invocation.getArgument(0)).get()).when(transactionService).required(any(Supplier.class));
    }

    @Test
    void testListAvailablesByAccountId() {
        when(repository.findAllByAccountId(any(), anyCollection(), any())).thenReturn(Set.of(CREDENTIAL));

        assertThat(credentialServiceUnderTest.listAvailablesByAccountId(ACCOUNT_ID, AUDIT)).isEqualTo(Set.of(CREDENTIAL));
        verify(credentialValidator, times(4))
                .isCredentialCloudPlatformValid(anyString(), eq(ACCOUNT_ID), any(CredentialType.class));
    }

    @Test
    void testGetValidPlatformsForAccountIdWhenAllEnabledAndEnvironmentCredential() {
        when(credentialValidator.isCredentialCloudPlatformValid(anyString(), eq(ACCOUNT_ID), any(CredentialType.class))).thenReturn(true);

        assertThat(credentialServiceUnderTest.getValidPlatformsForAccountId(ACCOUNT_ID, ENVIRONMENT)).containsOnly(AWS, AZURE, BLAH, BAZ);
    }

    @Test
    void testGetValidPlatformsForAccountIdWhenAllEnabledAndAuditCredential() {
        when(credentialValidator.isCredentialCloudPlatformValid(anyString(), eq(ACCOUNT_ID), any(CredentialType.class))).thenReturn(true);

        assertThat(credentialServiceUnderTest.getValidPlatformsForAccountId(ACCOUNT_ID, AUDIT)).containsOnly(AWS, AZURE, BLAH, BAZ);
    }

    @Test
    void testGetValidPlatformsForAccountIdWhenNoneEnabledAndEnvironmentCredential() {
        when(credentialValidator.isCredentialCloudPlatformValid(anyString(), eq(ACCOUNT_ID), any(CredentialType.class))).thenReturn(false);

        assertThat(credentialServiceUnderTest.getValidPlatformsForAccountId(ACCOUNT_ID, ENVIRONMENT)).isEmpty();
    }

    @Test
    void testGetValidPlatformsForAccountIdWhenNoneEnabledAndAuditCredential() {
        when(credentialValidator.isCredentialCloudPlatformValid(anyString(), eq(ACCOUNT_ID), any(CredentialType.class))).thenReturn(false);

        assertThat(credentialServiceUnderTest.getValidPlatformsForAccountId(ACCOUNT_ID, AUDIT)).isEmpty();
    }

    @Test
    void testGetValidPlatformsForAccountIdWhenAzureDisabledAndEnvironmentCredential() {
        when(credentialValidator.isCredentialCloudPlatformValid(eq(AZURE), eq(ACCOUNT_ID), any(CredentialType.class))).thenReturn(false);
        when(credentialValidator.isCredentialCloudPlatformValid(not(eq(AZURE)), eq(ACCOUNT_ID), any(CredentialType.class))).thenReturn(true);

        assertThat(credentialServiceUnderTest.getValidPlatformsForAccountId(ACCOUNT_ID, ENVIRONMENT)).containsOnly(AWS, BLAH, BAZ);
    }

    @Test
    void testGetValidPlatformsForAccountIdWhenAzureDisabledAndAuditCredential() {
        when(credentialValidator.isCredentialCloudPlatformValid(eq(AZURE), eq(ACCOUNT_ID), any(CredentialType.class))).thenReturn(false);
        when(credentialValidator.isCredentialCloudPlatformValid(not(eq(AZURE)), eq(ACCOUNT_ID), any(CredentialType.class))).thenReturn(true);

        assertThat(credentialServiceUnderTest.getValidPlatformsForAccountId(ACCOUNT_ID, AUDIT)).containsOnly(AWS, BLAH, BAZ);
    }

    @Test
    void testGetByNameForAccountIdHasResultAndEnvironmentCredential() {
        when(repository.findByNameAndAccountId(any(), any(), anyCollection(), any())).thenReturn(Optional.of(CREDENTIAL));
        assertEquals(CREDENTIAL, credentialServiceUnderTest.getByNameForAccountId(CREDENTIAL_NAME, ACCOUNT_ID, ENVIRONMENT));
    }

    @Test
    void testGetByNameForAccountIdHasResultAndAuditCredential() {
        when(repository.findByNameAndAccountId(any(), any(), anyCollection(), any())).thenReturn(Optional.of(CREDENTIAL));
        assertEquals(CREDENTIAL, credentialServiceUnderTest.getByNameForAccountId(CREDENTIAL_NAME, ACCOUNT_ID, AUDIT));
    }

    @Test
    void testGetByNameForAccountIdEmptyAndEnvironmentCredential() {
        when(repository.findByNameAndAccountId(any(), any(), anyCollection(), any())).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> credentialServiceUnderTest.getByNameForAccountId(CREDENTIAL_NAME, ACCOUNT_ID, ENVIRONMENT));
    }

    @Test
    void testGetByNameForAccountIdEmptyAndAuditCredential() {
        when(repository.findByNameAndAccountId(any(), any(), anyCollection(), any())).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> credentialServiceUnderTest.getByNameForAccountId(CREDENTIAL_NAME, ACCOUNT_ID, AUDIT));
    }

    @Test
    void testGetByCrnForAccountIdHasResultAndEnvironmentCredential() {
        when(repository.findByCrnAndAccountId(any(), any(), anyCollection(), any(), anyBoolean())).thenReturn(Optional.of(CREDENTIAL));
        assertEquals(CREDENTIAL, credentialServiceUnderTest.getByCrnForAccountId("123", ACCOUNT_ID, ENVIRONMENT));
    }

    @Test
    void testGetByCrnForAccountIdHasResultAndAuditCredential() {
        when(repository.findByCrnAndAccountId(any(), any(), anyCollection(), any(), anyBoolean())).thenReturn(Optional.of(CREDENTIAL));
        assertEquals(CREDENTIAL, credentialServiceUnderTest.getByCrnForAccountId("123", ACCOUNT_ID, AUDIT));
    }

    @Test
    void testGetByCrnForAccountIdEmptyAndEnvironmentCredential() {
        when(repository.findByCrnAndAccountId(any(), any(), anyCollection(), any(), anyBoolean())).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> credentialServiceUnderTest.getByCrnForAccountId("123", ACCOUNT_ID, ENVIRONMENT));
    }

    @Test
    void testGetByCrnForAccountIdEmptyAndAuditCredential() {
        when(repository.findByCrnAndAccountId(any(), any(), anyCollection(), any(), anyBoolean())).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> credentialServiceUnderTest.getByCrnForAccountId("123", ACCOUNT_ID, AUDIT));
    }

    @Test
    void testGetByEnvironmentCrnAndAccountIdHasResultAndAuditCredential() {
        when(repository.findByEnvironmentCrnAndAccountId(any(), any(), anyCollection(), any())).thenReturn(Optional.of(CREDENTIAL));
        assertEquals(CREDENTIAL, credentialServiceUnderTest.getByEnvironmentCrnAndAccountId("123", ACCOUNT_ID, AUDIT));
    }

    @Test
    void testGetByEnvironmentCrnAndAccountIdHasResultAndEnvironmentCredential() {
        when(repository.findByEnvironmentCrnAndAccountId(any(), any(), anyCollection(), any())).thenReturn(Optional.of(CREDENTIAL));
        assertEquals(CREDENTIAL, credentialServiceUnderTest.getByEnvironmentCrnAndAccountId("123", ACCOUNT_ID, ENVIRONMENT));
    }

    @Test
    void testGetByEnvironmentCrnAndAccountIdIdEmptyAndEnvironmentCredential() {
        when(repository.findByEnvironmentCrnAndAccountId(any(), any(), anyCollection(), any())).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> credentialServiceUnderTest.getByEnvironmentCrnAndAccountId("123", ACCOUNT_ID, ENVIRONMENT));
    }

    @Test
    void testGetByEnvironmentCrnAndAccountIdIdEmptyAndAuditCredential() {
        when(repository.findByEnvironmentCrnAndAccountId(any(), any(), anyCollection(), any())).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> credentialServiceUnderTest.getByEnvironmentCrnAndAccountId("123", ACCOUNT_ID, AUDIT));
    }

    @Test
    void testInteractiveLogin() throws JsonProcessingException {
        Map<String, String> testResult = Map.of("any", "any");
        when(credentialAdapter.interactiveLogin(eq(CREDENTIAL), anyString())).thenReturn(testResult);
        assertEquals(testResult, credentialServiceUnderTest.interactiveLogin("any", CREDENTIAL));
    }

    @Test
    void testInteractiveLoginBadRequestNoDeploymentAddress() throws JsonProcessingException {
        CREDENTIAL.setAttributes(getTestAttributes(STATE, null, REDIRECT_URL));
        when(credentialAdapter.interactiveLogin(eq(CREDENTIAL), anyString())).thenReturn(Map.of("any", "any"));
        assertThrows(BadRequestException.class, () -> credentialServiceUnderTest.interactiveLogin("any", CREDENTIAL));
    }

    @Test
    void testInteractiveLoginBadRequestNoCorrectAttributes() throws JsonProcessingException {
        CredentialAttributes azureAttributes = new CredentialAttributes();
        CREDENTIAL.setAttributes(JsonUtil.writeValueAsString(azureAttributes));
        when(credentialAdapter.interactiveLogin(eq(CREDENTIAL), anyString())).thenReturn(Map.of("any", "any"));
        assertThrows(BadRequestException.class, () -> credentialServiceUnderTest.interactiveLogin("any", CREDENTIAL));
    }

    @Test
    void testUpdateByAccountIdNotFoundAndEnvironmentCredential() {
        when(repository.findByNameAndAccountId(eq(CREDENTIAL_NAME), eq(ACCOUNT_ID), anyCollection(), any()))
                .thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> credentialServiceUnderTest.updateByAccountId(CREDENTIAL, ACCOUNT_ID, ENVIRONMENT));
    }

    @Test
    void testUpdateByAccountIdNotFoundAndAuditCredential() {
        when(repository.findByNameAndAccountId(eq(CREDENTIAL_NAME), eq(ACCOUNT_ID), anyCollection(), any()))
                .thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> credentialServiceUnderTest.updateByAccountId(CREDENTIAL, ACCOUNT_ID, AUDIT));
    }

    @Test
    void testUpdateByAccountIdModifyPlatformIsForbiddenAndEnvironmentCredential() {
        Credential result = new Credential();
        result.setCloudPlatform("anotherplatform");
        when(repository.findByNameAndAccountId(eq(CREDENTIAL_NAME), eq(ACCOUNT_ID), anyCollection(), any()))
                .thenReturn(Optional.of(result));
        when(credentialValidator.validateCredentialUpdate(any(), any(), any())).thenThrow(BadRequestException.class);
        assertThrows(BadRequestException.class, () -> credentialServiceUnderTest.updateByAccountId(CREDENTIAL, ACCOUNT_ID, ENVIRONMENT));
    }

    @Test
    void testUpdateByAccountIdModifyPlatformIsForbiddenAndAuditCredential() {
        Credential result = new Credential();
        result.setCloudPlatform("anotherplatform");
        when(repository.findByNameAndAccountId(eq(CREDENTIAL_NAME), eq(ACCOUNT_ID), anyCollection(), any()))
                .thenReturn(Optional.of(result));
        when(credentialValidator.validateCredentialUpdate(any(), any(), any())).thenThrow(BadRequestException.class);
        assertThrows(BadRequestException.class, () -> credentialServiceUnderTest.updateByAccountId(CREDENTIAL, ACCOUNT_ID, AUDIT));
    }

    @Test
    void testUpdateByAccountIdAndEnvironmentCredential() {
        Credential result = new Credential();
        result.setId(2L);
        result.setResourceCrn("this");
        result.setCloudPlatform(PLATFORM);
        CREDENTIAL.setId(1L);
        CREDENTIAL.setResourceCrn("that");
        CREDENTIAL.setCloudPlatform(PLATFORM);

        when(repository.findByNameAndAccountId(eq(CREDENTIAL_NAME), eq(ACCOUNT_ID), anyCollection(), any()))
                .thenReturn(Optional.of(result));
        when(credentialAdapter.verify(any(), anyString())).thenAnswer(i -> new CredentialVerification(i.getArgument(0), true));
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(credentialValidator.validateCredentialUpdate(any(Credential.class), any(Credential.class), any(CredentialType.class)))
                .thenReturn(ValidationResult.builder().build());

        Credential testResult = credentialServiceUnderTest.updateByAccountId(CREDENTIAL, ACCOUNT_ID, ENVIRONMENT);

        verify(repository).save(CREDENTIAL);
        assertEquals(2L, testResult.getId());
    }

    @Test
    void testUpdateByAccountIdAndAuditCredential() {
        Credential result = new Credential();
        result.setId(2L);
        result.setResourceCrn("this");
        result.setCloudPlatform(PLATFORM);
        CREDENTIAL.setId(1L);
        CREDENTIAL.setResourceCrn("that");
        CREDENTIAL.setCloudPlatform(PLATFORM);

        when(repository.findByNameAndAccountId(eq(CREDENTIAL_NAME), eq(ACCOUNT_ID), anyCollection(), any()))
                .thenReturn(Optional.of(result));
        when(credentialAdapter.verify(any(), anyString())).thenAnswer(i -> new CredentialVerification(i.getArgument(0), true));
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(credentialValidator.validateCredentialUpdate(any(Credential.class), any(Credential.class), any(CredentialType.class)))
                .thenReturn(ValidationResult.builder().build());

        Credential testResult = credentialServiceUnderTest.updateByAccountId(CREDENTIAL, ACCOUNT_ID, AUDIT);

        verify(repository).save(CREDENTIAL);
        assertEquals(2L, testResult.getId());
    }

    @Test
    void testCreateSameNameSameAccountIdNotSavedAndEnvironmentCredential() {
        when(repository.findByNameAndAccountId(eq(CREDENTIAL_NAME), eq(ACCOUNT_ID), anyCollection(), any()))
                .thenReturn(Optional.of(CREDENTIAL));
        assertThrows(BadRequestException.class, () -> credentialServiceUnderTest.create(CREDENTIAL, ACCOUNT_ID, USER_ID, ENVIRONMENT));
        verify(repository, never()).save(any());
    }

    @Test
    void testCreateSameNameSameAccountIdNotSavedAndAuditCredential() {
        when(repository.findByNameAndAccountId(eq(CREDENTIAL_NAME), eq(ACCOUNT_ID), anyCollection(), any()))
                .thenReturn(Optional.of(CREDENTIAL));
        assertThrows(BadRequestException.class, () -> credentialServiceUnderTest.create(CREDENTIAL, ACCOUNT_ID, USER_ID, AUDIT));
        verify(repository, never()).save(any());
    }

    @Test
    void testVerifyNothingChanged() {
        when(credentialAdapter.verify(any(), any())).thenReturn(new CredentialVerification(CREDENTIAL, false));
        credentialServiceUnderTest.verify(CREDENTIAL);
        verify(repository, never()).save(any());
    }

    @Test
    void testVerifyChanged() {
        when(credentialAdapter.verify(any(), any())).thenReturn(new CredentialVerification(CREDENTIAL, true));
        credentialServiceUnderTest.verify(CREDENTIAL);
        verify(repository).save(any());
    }

    @Test
    void testCreateAndEnvironmentCredential() {
        when(repository.findByNameAndAccountId(eq(CREDENTIAL_NAME), eq(ACCOUNT_ID), anyCollection(), any()))
                .thenReturn(Optional.empty());
        when(credentialAdapter.verify(any(), anyString(), anyBoolean())).thenAnswer(i -> new CredentialVerification(i.getArgument(0), true));
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));
        credentialServiceUnderTest.create(CREDENTIAL, ACCOUNT_ID, USER_ID, ENVIRONMENT);
        verify(credentialValidator).validateCredentialCloudPlatform(eq(PLATFORM), eq(USER_ID), any(CredentialType.class));
        verify(credentialValidator).validateParameters(any(), any());
        verify(repository).save(any());
    }

    @Test
    void testCreateAndAuditCredential() {
        when(repository.findByNameAndAccountId(eq(CREDENTIAL_NAME), eq(ACCOUNT_ID), anyCollection(), any()))
                .thenReturn(Optional.empty());
        when(credentialAdapter.verify(any(), anyString(), anyBoolean())).thenAnswer(i -> new CredentialVerification(i.getArgument(0), true));
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));
        credentialServiceUnderTest.create(CREDENTIAL, ACCOUNT_ID, USER_ID, AUDIT);
        verify(credentialValidator).validateCredentialCloudPlatform(eq(PLATFORM), eq(USER_ID), any(CredentialType.class));
        verify(credentialValidator).validateParameters(any(), any());
        verify(repository).save(any());
    }

    @Test
    void testCreateValidationErrorNotSavedAndEnvironmentCredential() {
        when(repository.findByNameAndAccountId(eq(CREDENTIAL_NAME), eq(ACCOUNT_ID), anyCollection(), any()))
                .thenReturn(Optional.empty());
        doThrow(BadRequestException.class).when(credentialValidator).validateParameters(any(), any());
        assertThrows(BadRequestException.class, () -> credentialServiceUnderTest.create(CREDENTIAL, ACCOUNT_ID, USER_ID, ENVIRONMENT));
        verify(repository, never()).save(any());
    }

    @Test
    void testCreateValidationErrorNotSavedAndAuditCredential() {
        when(repository.findByNameAndAccountId(eq(CREDENTIAL_NAME), eq(ACCOUNT_ID), anyCollection(), any()))
                .thenReturn(Optional.empty());
        doThrow(BadRequestException.class).when(credentialValidator).validateParameters(any(), any());
        assertThrows(BadRequestException.class, () -> credentialServiceUnderTest.create(CREDENTIAL, ACCOUNT_ID, USER_ID, AUDIT));
        verify(repository, never()).save(any());
    }

    @Test
    void testGetPrerequisitesAndEnvironmentCredential() {
        credentialServiceUnderTest.getPrerequisites(PLATFORM, false, DEPLOYMENT_ADDRESS, USER_ID, ENVIRONMENT);
        verify(credentialValidator).validateCredentialCloudPlatform(PLATFORM, USER_ID, ENVIRONMENT);
        verify(credentialPrerequisiteService).getPrerequisites(PLATFORM, false, DEPLOYMENT_ADDRESS, ENVIRONMENT);
    }

    @Test
    void testGetPrerequisitesAndAuditCredential() {
        credentialServiceUnderTest.getPrerequisites(PLATFORM, false, DEPLOYMENT_ADDRESS, USER_ID, AUDIT);
        verify(credentialValidator).validateCredentialCloudPlatform(PLATFORM, USER_ID, AUDIT);
        verify(credentialPrerequisiteService).getPrerequisites(PLATFORM, false, DEPLOYMENT_ADDRESS, AUDIT);
    }

    @Test
    void testInitCodeGrantFlow() {
        when(credentialAdapter.initCodeGrantFlow(any(), anyString())).thenReturn(CREDENTIAL);
        when(repository.save(any())).thenReturn(CREDENTIAL);
        String result = credentialServiceUnderTest.initCodeGrantFlow(ACCOUNT_ID, CREDENTIAL, USER_ID);
        assertEquals(REDIRECT_URL, result);
        verify(repository).save(eq(CREDENTIAL));
    }

    @Test
    void testInitCodeGrantFlowNoUrl() {
        CREDENTIAL.setAttributes(getTestAttributes(STATE, DEPLOYMENT_ADDRESS, null));

        when(credentialAdapter.initCodeGrantFlow(any(), anyString())).thenReturn(CREDENTIAL);
        when(repository.save(any())).thenReturn(CREDENTIAL);
        assertThrows(CredentialOperationException.class, () -> credentialServiceUnderTest.initCodeGrantFlow(ACCOUNT_ID, CREDENTIAL, USER_ID));
    }

    @Test
    void testInitCodeGrantFlowValidationErrorNotSaved() {
        doThrow(BadRequestException.class).when(credentialValidator).validateCredentialCloudPlatform(anyString(), anyString(), any(CredentialType.class));
        assertThrows(BadRequestException.class, () -> credentialServiceUnderTest.initCodeGrantFlow(ACCOUNT_ID, CREDENTIAL, USER_ID));
        verify(repository, never()).save(eq(CREDENTIAL));
    }

    @Test
    void testInitCodeGrantFlowAdapterErrorNotSaved() {
        when(credentialAdapter.initCodeGrantFlow(any(), anyString())).thenThrow(BadRequestException.class);
        assertThrows(BadRequestException.class, () -> credentialServiceUnderTest.initCodeGrantFlow(ACCOUNT_ID, CREDENTIAL, USER_ID));
        verify(repository, never()).save(eq(CREDENTIAL));
    }

    @Test
    void testInitCodeGrantFlowNoDeploymentAddress() {
        CREDENTIAL.setAttributes(getTestAttributes(STATE, null, REDIRECT_URL));
        when(credentialAdapter.initCodeGrantFlow(any(), anyString())).thenReturn(CREDENTIAL);
        when(repository.save(any())).thenReturn(CREDENTIAL);
        assertThrows(BadRequestException.class, () -> credentialServiceUnderTest.initCodeGrantFlow(ACCOUNT_ID, CREDENTIAL, USER_ID));
        verify(repository, never()).save(eq(CREDENTIAL));
    }

    @Test
    void testInitCodeGrantFlowExisting() {
        when(repository.findByNameAndAccountId(eq(CREDENTIAL_NAME), eq(ACCOUNT_ID), anyCollection(), any())).thenReturn(Optional.of(CREDENTIAL));
        when(credentialAdapter.initCodeGrantFlow(any(), anyString())).thenReturn(CREDENTIAL);
        when(repository.save(any())).thenReturn(CREDENTIAL);
        String result = credentialServiceUnderTest.initCodeGrantFlow(ACCOUNT_ID, CREDENTIAL_NAME);
        assertEquals(REDIRECT_URL, result);
        verify(repository).save(eq(CREDENTIAL));
        verify(secretService).delete(eq(getTestAttributes(STATE, DEPLOYMENT_ADDRESS, REDIRECT_URL)));
    }

    @Test
    void testInitCodeGrantFlowExistingButNot() {
        when(repository.findByNameAndAccountId(eq(CREDENTIAL_NAME), eq(ACCOUNT_ID), anyCollection(), any())).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> credentialServiceUnderTest.initCodeGrantFlow(ACCOUNT_ID, CREDENTIAL_NAME));
        verify(repository, never()).save(any());
        verify(secretService, never()).delete(anyString());
    }

    @Test
    void testInitCodeGrantFlowExistingWithoutGrantFlow() {
        CREDENTIAL.setAttributes(getTestAttributesWithCodeGrantFlow(null));
        when(repository.findByNameAndAccountId(eq(CREDENTIAL_NAME), eq(ACCOUNT_ID), anyCollection(), any())).thenReturn(Optional.of(CREDENTIAL));
        when(credentialAdapter.initCodeGrantFlow(any(), anyString())).thenReturn(CREDENTIAL);
        when(repository.save(any())).thenReturn(CREDENTIAL);
        assertThrows(UnsupportedOperationException.class, () -> credentialServiceUnderTest.initCodeGrantFlow(ACCOUNT_ID, CREDENTIAL_NAME));
        verify(repository, never()).save(any());
        verify(secretService, never()).delete(anyString());
    }

    @Test
    void testAuthorizeCodeGrantFlowNotFound() {
        when(repository.findAllByAccountId(eq(ACCOUNT_ID), anyCollection(), any())).thenReturn(Set.of());
        assertThrows(NotFoundException.class,
                () -> credentialServiceUnderTest.authorizeCodeGrantFlow(DIFFERENT_CODE, STATE, ACCOUNT_ID, "platform"));
        verify(repository, never()).save(any());
    }

    @Test
    void testAuthorizeCodeGrantFlowFoundButStateDoesNotMatch() {
        CREDENTIAL.setAttributes(getTestAttributes(DIFFERENT_STATE, DEPLOYMENT_ADDRESS, REDIRECT_URL));
        when(repository.findAllByAccountId(eq(ACCOUNT_ID), anyCollection(), any())).thenReturn(Set.of(CREDENTIAL));
        assertThrows(NotFoundException.class,
                () -> credentialServiceUnderTest.authorizeCodeGrantFlow(DIFFERENT_CODE, STATE, ACCOUNT_ID, "platform"));
        verify(repository, never()).save(any());
    }

    @Test
    void testAuthorizeCodeGrantFlowFoundStateMatches() throws IOException {
        when(repository.save(any())).thenReturn(CREDENTIAL);
        when(repository.findAllByAccountId(eq(ACCOUNT_ID), anyCollection(), any())).thenReturn(Set.of(CREDENTIAL));
        when(credentialAdapter.verify(any(), anyString())).thenAnswer(i -> new CredentialVerification(i.getArgument(0), true));

        Credential result =
                credentialServiceUnderTest.authorizeCodeGrantFlow(DIFFERENT_CODE, STATE, ACCOUNT_ID, "platform");
        CredentialAttributes resultAttributes = new Json(result.getAttributes()).get(CredentialAttributes.class);
        assertEquals(DIFFERENT_CODE, resultAttributes.getAzure().getCodeGrantFlowBased().getAuthorizationCode());
    }

    private String getTestAttributes(String state, String deploymentAddress, String url) {
        CodeGrantFlowAttributes codeGrantFlowBased = new CodeGrantFlowAttributes();
        codeGrantFlowBased.setDeploymentAddress(deploymentAddress);
        codeGrantFlowBased.setAppLoginUrl(url);
        codeGrantFlowBased.setCodeGrantFlowState(state);
        codeGrantFlowBased.setAuthorizationCode(CODE);
        return getTestAttributesWithCodeGrantFlow(codeGrantFlowBased);
    }

    private String getTestAttributesWithCodeGrantFlow(CodeGrantFlowAttributes codeGrantFlowAttributes) {
        CredentialAttributes credentialAttributes = new CredentialAttributes();
        AzureCredentialAttributes azureAttributes = new AzureCredentialAttributes();
        azureAttributes.setCodeGrantFlowBased(codeGrantFlowAttributes);
        credentialAttributes.setAzure(azureAttributes);
        return getAttributesAsString(credentialAttributes);
    }

    private String getAttributesAsString(CredentialAttributes credentialAttributes) {
        String credentialAttributesSecret = null;
        try {
            credentialAttributesSecret = JsonUtil.writeValueAsString(credentialAttributes);
        } catch (JsonProcessingException e) {

        }
        return credentialAttributesSecret;
    }

    @Configuration
    @Import(CredentialService.class)
    static class Config {
    }

}
