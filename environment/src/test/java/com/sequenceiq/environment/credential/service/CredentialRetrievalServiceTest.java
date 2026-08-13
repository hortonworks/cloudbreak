package com.sequenceiq.environment.credential.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.environment.credential.domain.Credential;
import com.sequenceiq.environment.credential.repository.CredentialRepository;
import com.sequenceiq.environment.credential.validation.CredentialValidator;

@ExtendWith(MockitoExtension.class)
class CredentialRetrievalServiceTest {

    private static final Long ENVIRONMENT_ID = 42L;

    private CredentialRetrievalService underTest;

    @Mock
    private CredentialRepository credentialRepository;

    @Mock
    private CredentialValidator credentialValidator;

    @BeforeEach
    void setUp() {
        underTest = new CredentialRetrievalService(credentialRepository, credentialValidator, Set.of("AWS", "AZURE"));
    }

    @Test
    void testFindByEnvironmentIdReturnsCredentialWhenFound() {
        Credential credential = new Credential();
        when(credentialRepository.findByEnvironmentId(ENVIRONMENT_ID)).thenReturn(Optional.of(credential));

        Credential result = underTest.findByEnvironmentId(ENVIRONMENT_ID);

        assertEquals(credential, result);
    }

    @Test
    void testFindByEnvironmentIdThrowsNotFoundExceptionWhenMissing() {
        when(credentialRepository.findByEnvironmentId(ENVIRONMENT_ID)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> underTest.findByEnvironmentId(ENVIRONMENT_ID));
    }
}
