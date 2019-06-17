package com.sequenceiq.environment.credential.service;

import static com.sequenceiq.cloudbreak.common.exception.NotFoundException.notFound;
import static com.sequenceiq.cloudbreak.util.NameUtil.generateArchiveName;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

import javax.ws.rs.BadRequestException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.message.CloudbreakMessagesService;
import com.sequenceiq.environment.configuration.EnabledPlatformProvider;
import com.sequenceiq.environment.credential.domain.Credential;
import com.sequenceiq.environment.credential.repository.CredentialRepository;
import com.sequenceiq.environment.environment.domain.EnvironmentView;
import com.sequenceiq.environment.environment.service.EnvironmentViewService;
import com.sequenceiq.notification.NotificationSender;
import com.sequenceiq.notification.ResourceEvent;

@Service
public class CredentialDeleteService extends AbstractCredentialService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CredentialDeleteService.class);

    private final CredentialRepository repository;

    private EnvironmentViewService environmentViewService;

    private EnabledPlatformProvider enabledPlatformProvider;

    public CredentialDeleteService(CredentialRepository repository,
            NotificationSender notificationSender,
            CloudbreakMessagesService messagesService,
            EnvironmentViewService environmentViewService,
            EnabledPlatformProvider enabledPlatformProvider) {
        super(notificationSender, messagesService);
        this.repository = repository;
        this.environmentViewService = environmentViewService;
        this.enabledPlatformProvider = enabledPlatformProvider;
    }

    public Set<Credential> deleteMultiple(Set<String> names, String accountId) {
        Set<Credential> deletedOnes = new LinkedHashSet<>();
        names.forEach(
                credentialName -> deletedOnes.add(
                        deleteByName(credentialName, accountId)));
        return deletedOnes;
    }

    public Credential deleteByName(String name, String accountId) {
        Credential credential = repository.findByNameAndAccountId(name, accountId,
                enabledPlatformProvider.enabledPlatforms())
                .orElseThrow(notFound(NOT_FOUND_FORMAT_MESS_NAME, name));
        checkEnvironmentsForDeletion(credential);
        LOGGER.debug("About to archive credential: {}", name);
        Credential archived = archiveCredential(credential);
        sendCredentialNotification(credential, ResourceEvent.CREDENTIAL_DELETED);
        return archived;
    }

    public Credential deleteByCrn(String crn, String accountId) {
        Credential credential = repository.findByNameAndAccountId(crn, accountId,
                enabledPlatformProvider.enabledPlatforms())
                .orElseThrow(notFound(NOT_FOUND_FORMAT_MESS_NAME, crn));
        checkEnvironmentsForDeletion(credential);
        LOGGER.debug("About to archive credential: {}", crn);
        Credential archived = archiveCredential(credential);
        sendCredentialNotification(credential, ResourceEvent.CREDENTIAL_DELETED);
        return archived;
    }

    private Credential archiveCredential(Credential credential) {
        credential.setName(generateArchiveName(credential.getName()));
        credential.setArchived(true);
        return repository.save(credential);
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
