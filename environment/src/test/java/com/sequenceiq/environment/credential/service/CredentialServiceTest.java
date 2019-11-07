package com.sequenceiq.environment.credential.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.AdditionalMatchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

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
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.message.CloudbreakMessagesService;
import com.sequenceiq.cloudbreak.service.secret.service.SecretService;
import com.sequenceiq.cloudbreak.util.ValidationResult;
import com.sequenceiq.environment.credential.attributes.CredentialAttributes;
import com.sequenceiq.environment.credential.attributes.azure.AzureCredentialAttributes;
import com.sequenceiq.environment.credential.attributes.azure.CodeGrantFlowAttributes;
import com.sequenceiq.environment.credential.domain.Credential;
import com.sequenceiq.environment.credential.exception.CredentialOperationException;
import com.sequenceiq.environment.credential.repository.CredentialRepository;
import com.sequenceiq.environment.credential.validation.CredentialValidator;
import com.sequenceiq.environment.credential.verification.CredentialVerification;
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

    @BeforeEach
    public void setupTestCredential() {
        CREDENTIAL.setName(CREDENTIAL_NAME);
        CREDENTIAL.setCloudPlatform(PLATFORM);
        String credentialAttributesSecret = getTestAttributes(STATE, DEPLOYMENT_ADDRESS, REDIRECT_URL);
        CREDENTIAL.setAttributes(credentialAttributesSecret);
    }

    @Test
    public void testListAvailablesByAccountId() {
        when(repository.findAllByAccountId(any(), anyCollection())).thenReturn(Set.of(CREDENTIAL));

        assertThat(credentialServiceUnderTest.listAvailablesByAccountId(ACCOUNT_ID)).isEqualTo(Set.of(CREDENTIAL));
        verify(credentialValidator, times(4)).isCredentialCloudPlatformValid(anyString(), eq(ACCOUNT_ID));
    }

    @Test
    void testGetValidPlatformsForAccountIdWhenAllEnabled() {
        when(credentialValidator.isCredentialCloudPlatformValid(anyString(), eq(ACCOUNT_ID))).thenReturn(true);

        assertThat(credentialServiceUnderTest.getValidPlatformsForAccountId(ACCOUNT_ID)).containsOnly(AWS, AZURE, BLAH, BAZ);
    }

    @Test
    void testGetValidPlatformsForAccountIdWhenNoneEnabled() {
        when(credentialValidator.isCredentialCloudPlatformValid(anyString(), eq(ACCOUNT_ID))).thenReturn(false);

        assertThat(credentialServiceUnderTest.getValidPlatformsForAccountId(ACCOUNT_ID)).isEmpty();
    }

    @Test
    void testGetValidPlatformsForAccountIdWhenAzureDisabled() {
        when(credentialValidator.isCredentialCloudPlatformValid(eq(AZURE), eq(ACCOUNT_ID))).thenReturn(false);
        when(credentialValidator.isCredentialCloudPlatformValid(not(eq(AZURE)), eq(ACCOUNT_ID))).thenReturn(true);

        assertThat(credentialServiceUnderTest.getValidPlatformsForAccountId(ACCOUNT_ID)).containsOnly(AWS, BLAH, BAZ);
    }

    @Test
    public void testGetByNameForAccountIdHasResult() {
        when(repository.findByNameAndAccountId(any(), any(), anyCollection())).thenReturn(Optional.of(CREDENTIAL));
        assertEquals(CREDENTIAL, credentialServiceUnderTest.getByNameForAccountId(CREDENTIAL_NAME, ACCOUNT_ID));
    }

    @Test
    public void testGetByNameForAccountIdEmpty() {
        when(repository.findByNameAndAccountId(any(), any(), anyCollection())).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> credentialServiceUnderTest.getByNameForAccountId(CREDENTIAL_NAME, ACCOUNT_ID));
    }

    @Test
    public void testGetByCrnForAccountIdHasResult() {
        when(repository.findByCrnAndAccountId(any(), any(), anyCollection())).thenReturn(Optional.of(CREDENTIAL));
        assertEquals(CREDENTIAL, credentialServiceUnderTest.getByCrnForAccountId("123", ACCOUNT_ID));
    }

    @Test
    public void testGetByCrnForAccountIdEmpty() {
        when(repository.findByCrnAndAccountId(any(), any(), anyCollection())).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> credentialServiceUnderTest.getByCrnForAccountId("123", ACCOUNT_ID));
    }

    @Test
    public void testGetByEnvironmentCrnAndAccountIdHasResult() {
        when(repository.findByEnvironmentCrnAndAccountId(any(), any(), anyCollection())).thenReturn(Optional.of(CREDENTIAL));
        assertEquals(CREDENTIAL, credentialServiceUnderTest.getByEnvironmentCrnAndAccountId("123", ACCOUNT_ID));
    }

    @Test
    public void testGetByEnvironmentCrnAndAccountIdIdEmpty() {
        when(repository.findByEnvironmentCrnAndAccountId(any(), any(), anyCollection())).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> credentialServiceUnderTest.getByEnvironmentCrnAndAccountId("123", ACCOUNT_ID));
    }

    @Test
    public void testInteractiveLogin() throws JsonProcessingException {
        Map<String, String> testResult = Map.of("any", "any");
        when(credentialAdapter.interactiveLogin(eq(CREDENTIAL), anyString(), anyString())).thenReturn(testResult);
        assertEquals(testResult, credentialServiceUnderTest.interactiveLogin("any", "any", CREDENTIAL));
    }

    @Test
    public void testInteractiveLoginBadRequestNoDeploymentAddress() throws JsonProcessingException {
        CREDENTIAL.setAttributes(getTestAttributes(STATE, null, REDIRECT_URL));
        when(credentialAdapter.interactiveLogin(eq(CREDENTIAL), anyString(), anyString())).thenReturn(Map.of("any", "any"));
        assertThrows(BadRequestException.class, () -> credentialServiceUnderTest.interactiveLogin("any", "any", CREDENTIAL));
    }

    @Test
    public void testInteractiveLoginBadRequestNoCorrectAttributes() throws JsonProcessingException {
        CredentialAttributes azureAttributes = new CredentialAttributes();
        CREDENTIAL.setAttributes(JsonUtil.writeValueAsString(azureAttributes));
        when(credentialAdapter.interactiveLogin(eq(CREDENTIAL), anyString(), anyString())).thenReturn(Map.of("any", "any"));
        assertThrows(BadRequestException.class, () -> credentialServiceUnderTest.interactiveLogin("any", "any", CREDENTIAL));
    }

    @Test
    public void testUpdateByAccountIdNotFound() {
        when(repository.findByNameAndAccountId(eq(CREDENTIAL_NAME), eq(ACCOUNT_ID), anyCollection()))
                .thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> credentialServiceUnderTest.updateByAccountId(CREDENTIAL, ACCOUNT_ID));
    }

    @Test
    public void testUpdateByAccountIdModifyPlatformIsForbidden() {
        Credential result = new Credential();
        result.setCloudPlatform("anotherplatform");
        when(repository.findByNameAndAccountId(eq(CREDENTIAL_NAME), eq(ACCOUNT_ID), anyCollection()))
                .thenReturn(Optional.of(result));
        when(credentialValidator.validateCredentialUpdate(any(), any())).thenThrow(BadRequestException.class);
        assertThrows(BadRequestException.class, () -> credentialServiceUnderTest.updateByAccountId(CREDENTIAL, ACCOUNT_ID));
    }

    @Test
    public void testUpdateByAccountId() {
        Credential result = new Credential();
        result.setId(2L);
        result.setResourceCrn("this");
        result.setCloudPlatform(PLATFORM);
        CREDENTIAL.setId(1L);
        CREDENTIAL.setResourceCrn("that");
        CREDENTIAL.setCloudPlatform(PLATFORM);

        when(repository.findByNameAndAccountId(eq(CREDENTIAL_NAME), eq(ACCOUNT_ID), anyCollection()))
                .thenReturn(Optional.of(result));
        when(credentialAdapter.verify(any(), anyString())).thenAnswer(i -> new CredentialVerification(i.getArgument(0), true));
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(credentialValidator.validateCredentialUpdate(any(Credential.class), any(Credential.class))).thenReturn(ValidationResult.builder().build());

        Credential testResult = credentialServiceUnderTest.updateByAccountId(CREDENTIAL, ACCOUNT_ID);

        verify(repository).save(CREDENTIAL);
        assertEquals(2L, testResult.getId());
    }

    @Test
    public void testCreateSameNameSameAccountIdNotSaved() {
        when(repository.findByNameAndAccountId(eq(CREDENTIAL_NAME), eq(ACCOUNT_ID), anyCollection()))
                .thenReturn(Optional.of(CREDENTIAL));
        assertThrows(BadRequestException.class, () -> credentialServiceUnderTest.create(CREDENTIAL, ACCOUNT_ID, USER_ID));
        verify(repository, never()).save(any());
    }

    @Test
    public void testVerifyNothingChanged() {
        when(credentialAdapter.verify(any(), any())).thenReturn(new CredentialVerification(CREDENTIAL, false));
        credentialServiceUnderTest.verify(CREDENTIAL);
        verify(repository, never()).save(any());
    }

    @Test
    public void testVerifyChanged() {
        when(credentialAdapter.verify(any(), any())).thenReturn(new CredentialVerification(CREDENTIAL, true));
        credentialServiceUnderTest.verify(CREDENTIAL);
        verify(repository).save(any());
    }

    @Test
    public void testCreate() {
        when(repository.findByNameAndAccountId(eq(CREDENTIAL_NAME), eq(ACCOUNT_ID), anyCollection()))
                .thenReturn(Optional.empty());
        when(credentialAdapter.verify(any(), anyString())).thenAnswer(i -> new CredentialVerification(i.getArgument(0), true));
        credentialServiceUnderTest.create(CREDENTIAL, ACCOUNT_ID, USER_ID);
        verify(credentialValidator).validateCredentialCloudPlatform(eq(PLATFORM), eq(USER_ID));
        verify(credentialValidator).validateParameters(any(), any());
        verify(repository).save(any());
    }

    @Test
    public void testCreateValidationErrorNotSaved() {
        when(repository.findByNameAndAccountId(eq(CREDENTIAL_NAME), eq(ACCOUNT_ID), anyCollection()))
                .thenReturn(Optional.empty());
        doThrow(BadRequestException.class).when(credentialValidator).validateParameters(any(), any());
        assertThrows(BadRequestException.class, () -> credentialServiceUnderTest.create(CREDENTIAL, ACCOUNT_ID, USER_ID));
        verify(repository, never()).save(any());
    }

    @Test
    public void testGetPrerequisites() {
        credentialServiceUnderTest.getPrerequisites(PLATFORM, DEPLOYMENT_ADDRESS, USER_ID);
        verify(credentialValidator).validateCredentialCloudPlatform(PLATFORM, USER_ID);
        verify(credentialPrerequisiteService).getPrerequisites(PLATFORM, DEPLOYMENT_ADDRESS);
    }

    @Test
    public void testInitCodeGrantFlow() {
        when(credentialAdapter.initCodeGrantFlow(any(), anyString(), anyString())).thenReturn(CREDENTIAL);
        when(repository.save(any())).thenReturn(CREDENTIAL);
        String result = credentialServiceUnderTest.initCodeGrantFlow(ACCOUNT_ID, CREDENTIAL, USER_ID);
        assertEquals(REDIRECT_URL, result);
        verify(repository).save(eq(CREDENTIAL));
    }

    @Test
    public void testInitCodeGrantFlowNoUrl() {
        CREDENTIAL.setAttributes(getTestAttributes(STATE, DEPLOYMENT_ADDRESS, null));

        when(credentialAdapter.initCodeGrantFlow(any(), anyString(), anyString())).thenReturn(CREDENTIAL);
        when(repository.save(any())).thenReturn(CREDENTIAL);
        assertThrows(CredentialOperationException.class, () -> credentialServiceUnderTest.initCodeGrantFlow(ACCOUNT_ID, CREDENTIAL, USER_ID));
    }

    @Test
    public void testInitCodeGrantFlowValidationErrorNotSaved() {
        doThrow(BadRequestException.class).when(credentialValidator).validateCredentialCloudPlatform(anyString(), anyString());
        assertThrows(BadRequestException.class, () -> credentialServiceUnderTest.initCodeGrantFlow(ACCOUNT_ID, CREDENTIAL, USER_ID));
        verify(repository, never()).save(eq(CREDENTIAL));
    }

    @Test
    public void testInitCodeGrantFlowAdapterErrorNotSaved() {
        when(credentialAdapter.initCodeGrantFlow(any(), anyString(), anyString())).thenThrow(BadRequestException.class);
        assertThrows(BadRequestException.class, () -> credentialServiceUnderTest.initCodeGrantFlow(ACCOUNT_ID, CREDENTIAL, USER_ID));
        verify(repository, never()).save(eq(CREDENTIAL));
    }

    @Test
    public void testInitCodeGrantFlowNoDeploymentAddress() {
        CREDENTIAL.setAttributes(getTestAttributes(STATE, null, REDIRECT_URL));
        when(credentialAdapter.initCodeGrantFlow(any(), anyString(), anyString())).thenReturn(CREDENTIAL);
        when(repository.save(any())).thenReturn(CREDENTIAL);
        assertThrows(BadRequestException.class, () -> credentialServiceUnderTest.initCodeGrantFlow(ACCOUNT_ID, CREDENTIAL, USER_ID));
        verify(repository, never()).save(eq(CREDENTIAL));
    }

    @Test
    public void testInitCodeGrantFlowExisting() {
        when(repository.findByNameAndAccountId(eq(CREDENTIAL_NAME), eq(ACCOUNT_ID), anyCollection())).thenReturn(Optional.of(CREDENTIAL));
        when(credentialAdapter.initCodeGrantFlow(any(), anyString(), anyString())).thenReturn(CREDENTIAL);
        when(repository.save(any())).thenReturn(CREDENTIAL);
        String result = credentialServiceUnderTest.initCodeGrantFlow(ACCOUNT_ID, CREDENTIAL_NAME, USER_ID);
        assertEquals(REDIRECT_URL, result);
        verify(repository).save(eq(CREDENTIAL));
        verify(secretService).delete(eq(getTestAttributes(STATE, DEPLOYMENT_ADDRESS, REDIRECT_URL)));
    }

    @Test
    public void testInitCodeGrantFlowExistingButNot() {
        when(repository.findByNameAndAccountId(eq(CREDENTIAL_NAME), eq(ACCOUNT_ID), anyCollection())).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> credentialServiceUnderTest.initCodeGrantFlow(ACCOUNT_ID, CREDENTIAL_NAME, USER_ID));
        verify(repository, never()).save(any());
        verify(secretService, never()).delete(anyString());
    }

    @Test
    public void testInitCodeGrantFlowExistingWithoutGrantFlow() {
        CREDENTIAL.setAttributes(getTestAttributesWithCodeGrantFlow(null));
        when(repository.findByNameAndAccountId(eq(CREDENTIAL_NAME), eq(ACCOUNT_ID), anyCollection())).thenReturn(Optional.of(CREDENTIAL));
        when(credentialAdapter.initCodeGrantFlow(any(), anyString(), anyString())).thenReturn(CREDENTIAL);
        when(repository.save(any())).thenReturn(CREDENTIAL);
        assertThrows(UnsupportedOperationException.class, () -> credentialServiceUnderTest.initCodeGrantFlow(ACCOUNT_ID, CREDENTIAL_NAME, USER_ID));
        verify(repository, never()).save(any());
        verify(secretService, never()).delete(anyString());
    }

    @Test
    public void testAuthorizeCodeGrantFlowNotFound() {
        when(repository.findAllByAccountId(eq(ACCOUNT_ID), anyCollection())).thenReturn(Set.of());
        assertThrows(NotFoundException.class,
                () -> credentialServiceUnderTest.authorizeCodeGrantFlow(DIFFERENT_CODE, STATE, ACCOUNT_ID, "platform"));
        verify(repository, never()).save(any());
    }

    @Test
    public void testAuthorizeCodeGrantFlowFoundButStateDoesNotMatch() {
        CREDENTIAL.setAttributes(getTestAttributes(DIFFERENT_STATE, DEPLOYMENT_ADDRESS, REDIRECT_URL));
        when(repository.findAllByAccountId(eq(ACCOUNT_ID), anyCollection())).thenReturn(Set.of(CREDENTIAL));
        assertThrows(NotFoundException.class,
                () -> credentialServiceUnderTest.authorizeCodeGrantFlow(DIFFERENT_CODE, STATE, ACCOUNT_ID, "platform"));
        verify(repository, never()).save(any());
    }

    @Test
    public void testAuthorizeCodeGrantFlowFoundStateMatches() throws IOException {
        when(repository.save(any())).thenReturn(CREDENTIAL);
        when(repository.findAllByAccountId(eq(ACCOUNT_ID), anyCollection())).thenReturn(Set.of(CREDENTIAL));
        when(credentialAdapter.verify(any(), anyString())).thenAnswer(i -> new CredentialVerification(i.getArgument(0), true));

        Credential result =
                credentialServiceUnderTest.authorizeCodeGrantFlow(DIFFERENT_CODE, STATE, ACCOUNT_ID, "platform");
        CredentialAttributes resultAttributes = new Json(result.getAttributes()).get(CredentialAttributes.class);
        assertEquals(resultAttributes.getAzure().getCodeGrantFlowBased().getAuthorizationCode(), DIFFERENT_CODE);
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
