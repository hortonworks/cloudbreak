package com.sequenceiq.environment.credential.service;

import static com.sequenceiq.common.model.CredentialType.ENVIRONMENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.service.secret.service.SecretService;
import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.common.model.CredentialType;
import com.sequenceiq.environment.credential.domain.Credential;
import com.sequenceiq.environment.credential.repository.CredentialRepository;
import com.sequenceiq.environment.credential.validation.CredentialValidator;
import com.sequenceiq.environment.credential.verification.CredentialVerification;

@ExtendWith(MockitoExtension.class)
public class CredentialUpdateServiceTest {

    private CredentialUpdateService underTest;

    @Mock
    private CredentialRepository credentialRepository;

    @Mock
    private CredentialValidator credentialValidator;

    @Mock
    private ServiceProviderCredentialAdapter credentialAdapter;

    @Mock
    private SecretService secretService;

    @Mock
    private CredentialNotificationService credentialNotificationService;

    @BeforeEach
    void setUp() {
        underTest = new CredentialUpdateService(credentialRepository, credentialValidator, credentialAdapter, secretService, credentialNotificationService,
                Set.of("AWS", "AZURE"));
    }

    @Test
    void testUpdateByAccountId() {
        Credential credential = createCredential("testCredential", ENVIRONMENT);
        credential.setAttributes("{\"enginePath\":\"secret\",\"engineClass\":\"com.sequenceiq.cloudbreak.service.secret.vault.VaultKvV2Engine\"," +
                "\"path\":\"app/path\",\"version\":1}");
        when(credentialRepository.findByNameAndAccountId(eq(credential.getName()), eq("testAccountId"), any(), eq(ENVIRONMENT)))
                .thenReturn(Optional.of(credential));
        when(credentialValidator.validateCredentialUpdate(any(), any(), any()))
                .thenReturn(ValidationResult.builder().build());
        CredentialVerification credentialVerification = new CredentialVerification(credential, true);
        when(credentialAdapter.verify(any(), anyString(), eq(true))).thenReturn(credentialVerification);
        when(credentialRepository.save(any())).thenReturn(credential);

        Credential result = underTest.updateByAccountId(credential, "testAccountId", ENVIRONMENT);

        assertEquals(credential, result);

        verify(credentialRepository, times(1)).save(credential);
        verify(secretService, times(1)).deleteByVaultSecretJson(credential.getAttributesSecret());
    }

    @Test
    void testUpdateByAccountIdNotFound() {
        Credential credential = new Credential();
        credential.setName("testCredential");
        String accountId = "testAccountId";
        CredentialType type = CredentialType.AUDIT;

        when(credentialRepository.findByNameAndAccountId(anyString(), anyString(), any(), any()))
                .thenReturn(Optional.empty());

        NotFoundException nfe = assertThrows(NotFoundException.class, () -> underTest.updateByAccountId(credential, accountId, type));
        assertEquals("Credential with name: 'testCredential' not found.", nfe.getMessage());
    }

    private Credential createCredential(String name, CredentialType type) {
        Credential credential = new Credential();
        credential.setName(name);
        credential.setType(type);
        return credential;
    }
}