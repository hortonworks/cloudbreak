package com.sequenceiq.environment.credential.service;

import static com.sequenceiq.cloudbreak.common.exception.NotFoundException.notFound;
import static com.sequenceiq.environment.credential.service.CredentialNotificationService.NOT_FOUND_FORMAT_MESS_NAME;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import jakarta.ws.rs.BadRequestException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.google.common.collect.Sets;
import com.sequenceiq.authorization.service.list.ResourceWithId;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.common.model.CredentialType;
import com.sequenceiq.environment.credential.domain.Credential;
import com.sequenceiq.environment.credential.repository.CredentialRepository;
import com.sequenceiq.environment.credential.validation.CredentialValidator;

@Service
public class CredentialRetrievalService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CredentialRetrievalService.class);

    private final CredentialRepository credentialRepository;

    private final CredentialValidator credentialValidator;

    private final Set<String> enabledPlatforms;

    protected CredentialRetrievalService(CredentialRepository credentialRepository, CredentialValidator credentialValidator,
            @Value("${environment.enabledplatforms}") Set<String> enabledPlatforms) {
        this.credentialRepository = credentialRepository;
        this.credentialValidator = credentialValidator;
        this.enabledPlatforms = enabledPlatforms;

    }

    public Set<Credential> listAvailablesByAccountId(String accountId, CredentialType type) {
        return credentialRepository.findAllByAccountId(accountId, credentialValidator.getValidPlatformsForAccountId(accountId, type), type);
    }

    public List<ResourceWithId> findAsAuthorizationResourcesInAccountByType(String accountId, CredentialType type) {
        return credentialRepository.findAsAuthorizationResourcesInAccountByType(accountId,
                credentialValidator.getValidPlatformsForAccountId(accountId, type), type);
    }

    public Set<Credential> findAllById(Iterable<Long> ids) {
        return Sets.newLinkedHashSet(credentialRepository.findAllById(ids));
    }

    public Credential getByNameForAccountId(String name, String accountId, CredentialType type) {
        return extractCredential(getOptionalByNameForAccountId(name, accountId, type), name);
    }

    public Optional<Credential> getOptionalByNameForAccountId(String name, String accountId, CredentialType type) {
        return credentialRepository.findByNameAndAccountId(name, accountId, enabledPlatforms, type);
    }

    public Credential getByCrnForAccountId(String crn, String accountId, CredentialType type) {
        return getByCrnForAccountId(crn, accountId, type, false);
    }

    public Credential getByCrnForAccountId(String crn, String accountId, CredentialType type, boolean queryArchived) {
        return extractCredential(getOptionalByCrnForAccountId(crn, accountId, type, queryArchived), crn);
    }

    public Optional<Credential> getOptionalByCrnForAccountId(String crn, String accountId, CredentialType type) {
        return getOptionalByCrnForAccountId(crn, accountId, type, false);
    }

    public Optional<Credential> getOptionalByCrnForAccountId(String crn, String accountId, CredentialType type, boolean queryArchived) {
        Optional<Credential> credential = credentialRepository.findByCrnAndAccountId(crn, accountId, enabledPlatforms, type, false);
        if (queryArchived && credential.isEmpty()) {
            credential = credentialRepository.findByCrnAndAccountId(crn, accountId, enabledPlatforms, type, true);
        }
        return credential;
    }

    public Credential extractCredential(Optional<Credential> credential, String resourceIdentifier) {
        return credential.orElseThrow(notFound(NOT_FOUND_FORMAT_MESS_NAME, resourceIdentifier));
    }

    public Credential getByEnvironmentCrnAndAccountId(String environmentCrn, String accountId, CredentialType type) {
        return credentialRepository.findByEnvironmentCrnAndAccountId(environmentCrn, accountId, enabledPlatforms, type)
                .orElseThrow(notFound("Credential with environmentCrn:", environmentCrn));
    }

    public Credential getByEnvironmentNameAndAccountId(String environmentName, String accountId, CredentialType type) {
        return credentialRepository.findByEnvironmentNameAndAccountId(environmentName, accountId, enabledPlatforms, type)
                .orElseThrow(notFound("Credential with environmentName:", environmentName));
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

    Optional<Credential> findByNameAndAccountId(String name, String accountId, Collection<String> cloudPlatforms, CredentialType type) {
        return credentialRepository.findByNameAndAccountId(name, accountId, cloudPlatforms, type);
    }

    Optional<Credential> findByCrnAndAccountId(String crn, String accountId, Collection<String> cloudPlatforms, CredentialType type) {
        return credentialRepository.findByCrnAndAccountId(crn, accountId, cloudPlatforms, type, false);
    }

    public Map<String, Optional<String>> getNamesByCrnsForMessage(Collection<String> crns) {
        Map<String, Optional<String>> result = new HashMap<>();
        credentialRepository.findResourceNamesByCrnAndAccountId(crns, ThreadBasedUserCrnProvider.getAccountId(), enabledPlatforms).stream()
                .forEach(nameAndCrn -> result.put(nameAndCrn.getCrn(), Optional.ofNullable(nameAndCrn.getName())));
        return result;
    }

}
