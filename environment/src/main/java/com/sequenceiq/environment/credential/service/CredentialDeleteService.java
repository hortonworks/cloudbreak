package com.sequenceiq.environment.credential.service;

import static com.sequenceiq.cloudbreak.common.exception.NotFoundException.notFound;
import static com.sequenceiq.cloudbreak.util.NameUtil.generateArchiveName;
import static com.sequenceiq.environment.credential.service.CredentialNotificationService.NOT_FOUND_FORMAT_MESS_NAME;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.ws.rs.BadRequestException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.sequenceiq.authorization.service.OwnerAssignmentService;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.common.model.CredentialType;
import com.sequenceiq.environment.credential.domain.Credential;
import com.sequenceiq.environment.credential.repository.CredentialRepository;
import com.sequenceiq.environment.environment.domain.EnvironmentView;
import com.sequenceiq.environment.environment.service.EnvironmentViewService;

@Service
public class CredentialDeleteService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CredentialDeleteService.class);

    private final CredentialRetrievalService credentialRetrievalService;

    private final EnvironmentViewService environmentViewService;

    private final OwnerAssignmentService ownerAssignmentService;

    private final CredentialRepository credentialRepository;

    private final CredentialNotificationService credentialNotificationService;

    private final Set<String> enabledPlatforms;

    public CredentialDeleteService(CredentialRetrievalService credentialRetrievalService, CredentialNotificationService credentialNotificationService,
            EnvironmentViewService environmentViewService,
            OwnerAssignmentService ownerAssignmentService,
            CredentialRepository credentialRepository,
            @Value("${environment.enabledplatforms}") Set<String> enabledPlatforms) {
        this.credentialNotificationService = credentialNotificationService;
        this.credentialRetrievalService = credentialRetrievalService;
        this.environmentViewService = environmentViewService;
        this.ownerAssignmentService = ownerAssignmentService;
        this.credentialRepository = credentialRepository;
        this.enabledPlatforms = enabledPlatforms;
    }

    public Set<Credential> deleteMultiple(Set<String> names, String accountId, CredentialType type) {
        Set<Credential> deletedOnes = new LinkedHashSet<>();
        names.forEach(credentialName -> {
            try {
                deletedOnes.add(deleteByName(credentialName, accountId, type));
            } catch (Exception ex) {
                LOGGER.debug("Could not delete Credential with name {} because: {}", credentialName, ex.getMessage());
            }
        });
        return deletedOnes;
    }

    public Credential deleteByName(String name, String accountId, CredentialType type) {
        Credential credential = credentialRetrievalService.findByNameAndAccountId(name, accountId, enabledPlatforms, type)
                .orElseThrow(notFound(NOT_FOUND_FORMAT_MESS_NAME, name));
        checkEnvironmentsForDeletion(credential);
        LOGGER.debug("About to archive credential: {}", name);
        Credential archived = archiveCredential(credential);
        ownerAssignmentService.notifyResourceDeleted(archived.getResourceCrn());
        credentialNotificationService.send(credential, ResourceEvent.CREDENTIAL_DELETED);
        return archived;
    }

    public Credential deleteByCrn(String crn, String accountId, CredentialType type) {
        Credential credential = credentialRetrievalService.findByCrnAndAccountId(crn, accountId, enabledPlatforms, type)
                .orElseThrow(notFound(NOT_FOUND_FORMAT_MESS_NAME, crn));
        checkEnvironmentsForDeletion(credential);
        LOGGER.debug("About to archive credential: {}", crn);
        Credential archived = archiveCredential(credential);
        ownerAssignmentService.notifyResourceDeleted(archived.getResourceCrn());
        credentialNotificationService.send(credential, ResourceEvent.CREDENTIAL_DELETED);
        return archived;
    }

    private Credential archiveCredential(Credential credential) {
        credential.setName(generateArchiveName(credential.getName()));
        credential.setArchived(true);
        return credentialRepository.save(credential);
    }

    private void checkEnvironmentsForDeletion(Credential credential) {
        Set<EnvironmentView> environments = environmentViewService.findAllByCredentialId(credential.getId());
        if (!environments.isEmpty()) {
            String environmentList = environments.stream().map(EnvironmentView::getName).collect(Collectors.joining(", "));
            String message = "Credential '%s' cannot be deleted because the following environments are using it: [%s].";
            throw new BadRequestException(String.format(message, credential.getName(), environmentList));
        }
    }

}
