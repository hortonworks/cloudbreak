package com.sequenceiq.environment.credential.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import jakarta.ws.rs.BadRequestException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.common.model.CredentialType;
import com.sequenceiq.environment.credential.domain.Credential;
import com.sequenceiq.environment.credential.validation.CredentialValidator;
import com.sequenceiq.environment.environment.verification.PolicyValidationErrorResponseConverter;

@ExtendWith(MockitoExtension.class)
class CredentialServiceTest {

    private static final String CREDENTIAL_NAME = "credentialName";

    private static final String ACCOUNT_ID = "accountId";

    @Mock
    private CredentialValidator credentialValidator;

    @Mock
    private ServiceProviderCredentialAdapter credentialAdapter;

    @Mock
    private CredentialPrerequisiteService credentialPrerequisiteService;

    @Mock
    private PolicyValidationErrorResponseConverter policyValidationErrorResponseConverter;

    @Mock
    private CredentialCreateService credentialCreateService;

    @Mock
    private CredentialRetrievalService credentialRetrievalService;

    @Mock
    private CredentialUpdateService credentialUpdateService;

    @InjectMocks
    private CredentialService underTest;

    @Test
    void testGetCredentialForEnvCreationWhenCredentialIsFound() {
        Credential credential = mock();
        when(credentialRetrievalService.getByNameForAccountId(CREDENTIAL_NAME, ACCOUNT_ID, CredentialType.ENVIRONMENT)).thenReturn(credential);

        Credential result = underTest.getCredentialForEnvCreation(CREDENTIAL_NAME, ACCOUNT_ID, CredentialType.ENVIRONMENT);

        assertEquals(credential, result);
    }

    @Test
    void testGetCredentialForEnvCreationWhenCredentialIsNotFound() {
        when(credentialRetrievalService.getByNameForAccountId(CREDENTIAL_NAME, ACCOUNT_ID, CredentialType.ENVIRONMENT)).thenThrow(NotFoundException.class);

        assertThrows(BadRequestException.class, () -> underTest.getCredentialForEnvCreation(CREDENTIAL_NAME, ACCOUNT_ID, CredentialType.ENVIRONMENT));
    }

    @Test
    void testGetCredentialForEnvCreationWhenCredentialNameIsNull() {
        assertThrows(BadRequestException.class, () -> underTest.getCredentialForEnvCreation(null, ACCOUNT_ID, CredentialType.ENVIRONMENT));
        verifyNoInteractions(credentialRetrievalService);
    }

    @Test
    void testGetCredentialForEnvCreationWhenCredentialNameIsEmpty() {
        assertThrows(BadRequestException.class, () -> underTest.getCredentialForEnvCreation("", ACCOUNT_ID, CredentialType.ENVIRONMENT));
        verifyNoInteractions(credentialRetrievalService);
    }
}
