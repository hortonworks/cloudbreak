package com.sequenceiq.environment.credential.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;
import javax.ws.rs.BadRequestException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.message.CloudbreakMessagesService;
import com.sequenceiq.cloudbreak.service.secret.service.SecretService;
import com.sequenceiq.cloudbreak.util.ValidationResult;
import com.sequenceiq.environment.credential.attributes.CredentialAttributes;
import com.sequenceiq.environment.credential.attributes.azure.AzureCredentialAttributes;
import com.sequenceiq.environment.credential.attributes.azure.CodeGrantFlowAttributes;
import com.sequenceiq.environment.credential.domain.Credential;
import com.sequenceiq.environment.credential.repository.CredentialRepository;
import com.sequenceiq.environment.credential.validation.CredentialValidator;
import com.sequenceiq.notification.NotificationSender;

@ExtendWith(SpringExtension.class)
public class CredentialServiceTest {
    private static final Credential CREDENTIAL = new Credential();

    private static final String PLATFORM = "PLATFORM";

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

    @Test
    public void testListAvailablesByAccountId() {
        when(repository.findAllByAccountId(any(), anyCollection())).thenReturn(Set.of(CREDENTIAL));
        assertEquals(Set.of(CREDENTIAL), credentialServiceUnderTest.listAvailablesByAccountId("123"));
    }

    @Test
    public void testGetByNameForAccountIdHasResult() {
        when(repository.findByNameAndAccountId(any(), any(), anyCollection())).thenReturn(Optional.of(CREDENTIAL));
        assertEquals(CREDENTIAL, credentialServiceUnderTest.getByNameForAccountId("123", "123"));
    }

    @Test
    public void testGetByNameForAccountIdEmpty() {
        when(repository.findByNameAndAccountId(any(), any(), anyCollection())).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> credentialServiceUnderTest.getByNameForAccountId("123", "123"));
    }

    @Test
    public void testGetByCrnForAccountIdHasResult() {
        when(repository.findByCrnAndAccountId(any(), any(), anyCollection())).thenReturn(Optional.of(CREDENTIAL));
        assertEquals(CREDENTIAL, credentialServiceUnderTest.getByCrnForAccountId("123", "123"));
    }

    @Test
    public void testGetByCrnForAccountIdEmpty() {
        when(repository.findByCrnAndAccountId(any(), any(), anyCollection())).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> credentialServiceUnderTest.getByCrnForAccountId("123", "123"));
    }

    @Test
    public void testGetByEnvironmentCrnAndAccountIdHasResult() {
        when(repository.findByEnvironmentCrnAndAccountId(any(), any(), anyCollection())).thenReturn(Optional.of(CREDENTIAL));
        assertEquals(CREDENTIAL, credentialServiceUnderTest.getByEnvironmentCrnAndAccountId("123", "123"));
    }

    @Test
    public void testGetByEnvironmentCrnAndAccountIdIdEmpty() {
        when(repository.findByEnvironmentCrnAndAccountId(any(), any(), anyCollection())).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> credentialServiceUnderTest.getByEnvironmentCrnAndAccountId("123", "123"));
    }

    @Test
    public void testInteractiveLogin() throws JsonProcessingException {
        CredentialAttributes azureAttributes = new CredentialAttributes();
        AzureCredentialAttributes azureCredentialAttributes = new AzureCredentialAttributes();
        CodeGrantFlowAttributes codeGrantFlowBased = new CodeGrantFlowAttributes();
        azureCredentialAttributes.setCodeGrantFlowBased(codeGrantFlowBased);
        azureAttributes.setAzure(azureCredentialAttributes);
        codeGrantFlowBased.setDeploymentAddress("address");
        CREDENTIAL.setAttributes(JsonUtil.writeValueAsString(azureAttributes));
        Map<String, String> testResult = Map.of("any", "any");
        when(credentialAdapter.interactiveLogin(eq(CREDENTIAL), anyString(), anyString())).thenReturn(testResult);
        assertEquals(testResult, credentialServiceUnderTest.interactiveLogin("any", "any", CREDENTIAL));
    }

    @Test
    public void testInteractiveLoginBadRequestNoDeploymentAddress() throws JsonProcessingException {
        CredentialAttributes azureAttributes = new CredentialAttributes();
        AzureCredentialAttributes azureCredentialAttributes = new AzureCredentialAttributes();
        CodeGrantFlowAttributes codeGrantFlowBased = new CodeGrantFlowAttributes();
        azureCredentialAttributes.setCodeGrantFlowBased(codeGrantFlowBased);
        azureAttributes.setAzure(azureCredentialAttributes);
        CREDENTIAL.setAttributes(JsonUtil.writeValueAsString(azureAttributes));
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
        CREDENTIAL.setName("name");
        when(repository.findByNameAndAccountId(eq("name"), eq("123"), anyCollection()))
                .thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> credentialServiceUnderTest.updateByAccountId(CREDENTIAL, "123"));
    }

    @Test
    public void testUpdateByAccountIdModifyPlatformIsForbidden() {
        CREDENTIAL.setName("name");
        when(repository.findByNameAndAccountId(eq("name"), eq("123"), anyCollection()))
                .thenThrow(BadRequestException.class);

        assertThrows(BadRequestException.class, () -> credentialServiceUnderTest.updateByAccountId(CREDENTIAL, "123"));
    }

    @Test
    public void testUpdateByAccountId() {
        Credential result = new Credential();
        result.setId(2L);
        result.setResourceCrn("this");
        CREDENTIAL.setName("name");
        CREDENTIAL.setId(1L);
        CREDENTIAL.setResourceCrn("that");

        when(repository.findByNameAndAccountId(eq("name"), eq("123"), anyCollection()))
                .thenReturn(Optional.of(result));
        when(credentialAdapter.verify(any(), anyString())).thenAnswer(i -> i.getArgument(0));
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(credentialValidator.validateCredentialUpdate(any(Credential.class), any(Credential.class))).thenReturn(ValidationResult.builder().build());

        Credential testResult = credentialServiceUnderTest.updateByAccountId(CREDENTIAL, "123");

        verify(repository).save(CREDENTIAL);
        assertEquals(2L, testResult.getId());
    }

    @Test()
    public void testCreateWithRetry() {
        //TODO
    }

    @Test
    public void testCreate() {
        //TODO
    }

    @Test
    public void testGetPrerequisites() {
        credentialServiceUnderTest.getPrerequisites(PLATFORM, "address");
        verify(credentialValidator).validateCredentialCloudPlatform(PLATFORM);
        verify(credentialPrerequisiteService).getPrerequisites(PLATFORM, "address");
    }

    @Test
    public void testInitCodeGrantFlow() {
        //TODO
    }

    @Test
    public void testInitCodeGrantFlow2() {
        //TODO
    }

    @Test
    public void testAuthorizeCodeGrantFlow() {
        //TODO
    }

    @Configuration
    @Import(CredentialService.class)
    static class Config {
    }

}
