package com.sequenceiq.environment.credential.service;

import static com.sequenceiq.cloudbreak.common.exception.NotFoundException.notFound;
import static com.sequenceiq.environment.credential.service.CredentialNotificationService.NOT_FOUND_FORMAT_MESS_NAME;

import java.util.Set;

import jakarta.ws.rs.BadRequestException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.cloudbreak.service.secret.service.SecretService;
import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.common.model.CredentialType;
import com.sequenceiq.environment.credential.domain.Credential;
import com.sequenceiq.environment.credential.repository.CredentialRepository;
import com.sequenceiq.environment.credential.validation.CredentialValidator;

@Service
public class CredentialUpdateService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CredentialUpdateService.class);

    private final CredentialRepository credentialRepository;

    private final CredentialValidator credentialValidator;

    private final ServiceProviderCredentialAdapter credentialAdapter;

    private final SecretService secretService;

    private final CredentialNotificationService credentialNotificationService;

    private final Set<String> enabledPlatforms;

    protected CredentialUpdateService(CredentialRepository credentialRepository, CredentialValidator credentialValidator,
            ServiceProviderCredentialAdapter credentialAdapter, SecretService secretService, CredentialNotificationService credentialNotificationService,
            @Value("${environment.enabledplatforms}") Set<String> enabledPlatforms) {
        this.credentialNotificationService = credentialNotificationService;
        this.credentialRepository = credentialRepository;
        this.credentialValidator = credentialValidator;
        this.credentialAdapter = credentialAdapter;
        this.secretService = secretService;
        this.enabledPlatforms = enabledPlatforms;
    }

    public Credential updateByAccountId(Credential credential, String accountId, CredentialType type) {
        Credential original = getCredentialAndValidateUpdate(credential, accountId, type);
        credential.setId(original.getId());
        credential.setAccountId(accountId);
        credential.setResourceCrn(original.getResourceCrn());
        credential.setCreator(original.getCreator());

        Credential updated = update(credentialAdapter.verify(credential, accountId, true).getCredential());
        secretService.deleteByVaultSecretJson(original.getAttributesSecret());
        credentialNotificationService.send(credential, ResourceEvent.CREDENTIAL_MODIFIED);
        return updated;
    }

    private Credential getCredentialAndValidateUpdate(Credential credential, String accountId, CredentialType type) {
        Credential original = credentialRepository.findByNameAndAccountId(credential.getName(), accountId, enabledPlatforms, type)
                .orElseThrow(notFound(NOT_FOUND_FORMAT_MESS_NAME, credential.getName()));
        ValidationResult validationResult = credentialValidator.validateCredentialUpdate(original, credential,
                type);
        if (validationResult.hasError()) {
            throw new BadRequestException(validationResult.getFormattedErrors());
        }
        return original;
    }

    public Credential update(Credential credential) {
        return credentialRepository.save(credential);
    }
}
