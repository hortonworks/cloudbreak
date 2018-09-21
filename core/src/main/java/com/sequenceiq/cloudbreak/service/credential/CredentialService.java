package com.sequenceiq.cloudbreak.service.credential;

import static com.sequenceiq.cloudbreak.controller.exception.NotFoundException.notFound;
import static com.sequenceiq.cloudbreak.util.NameUtil.generateArchiveName;

import java.util.Date;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.Resource;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.convert.ConversionService;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.model.CloudbreakEventsJson;
import com.sequenceiq.cloudbreak.api.model.CredentialRequest;
import com.sequenceiq.cloudbreak.api.model.CredentialResponse;
import com.sequenceiq.cloudbreak.api.model.v3.credential.CredentialPrerequisites;
import com.sequenceiq.cloudbreak.authorization.WorkspaceResource;
import com.sequenceiq.cloudbreak.common.type.ResourceEvent;
import com.sequenceiq.cloudbreak.controller.exception.BadRequestException;
import com.sequenceiq.cloudbreak.controller.exception.NotFoundException;
import com.sequenceiq.cloudbreak.controller.validation.credential.CredentialValidator;
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.domain.Topology;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.workspace.User;
import com.sequenceiq.cloudbreak.domain.workspace.Workspace;
import com.sequenceiq.cloudbreak.repository.CredentialRepository;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.repository.workspace.WorkspaceResourceRepository;
import com.sequenceiq.cloudbreak.service.AbstractWorkspaceAwareResourceService;
import com.sequenceiq.cloudbreak.service.account.AccountPreferencesService;
import com.sequenceiq.cloudbreak.service.messages.CloudbreakMessagesService;
import com.sequenceiq.cloudbreak.service.notification.Notification;
import com.sequenceiq.cloudbreak.service.notification.NotificationSender;
import com.sequenceiq.cloudbreak.service.stack.connector.adapter.ServiceProviderCredentialAdapter;
import com.sequenceiq.cloudbreak.service.user.UserProfileHandler;
import com.sequenceiq.cloudbreak.service.workspace.WorkspaceService;

@Service
public class CredentialService extends AbstractWorkspaceAwareResourceService<Credential> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CredentialService.class);

    private static final String NOT_FOUND_FORMAT_MESS_ID = "Credential with id:";

    private static final String NOT_FOUND_FORMAT_MESS_NAME = "Credential with name:";

    @Resource
    @Qualifier("conversionService")
    private ConversionService conversionService;

    @Inject
    private CredentialRepository credentialRepository;

    @Inject
    private StackRepository stackRepository;

    @Inject
    private ServiceProviderCredentialAdapter credentialAdapter;

    @Inject
    private UserProfileHandler userProfileHandler;

    @Inject
    private AccountPreferencesService accountPreferencesService;

    @Inject
    private NotificationSender notificationSender;

    @Inject
    private CloudbreakMessagesService messagesService;

    @Inject
    private WorkspaceService workspaceService;

    @Inject
    private CredentialValidator credentialValidator;

    @Inject
    private CredentialPrerequisiteService credentialPrerequisiteService;

    public Set<Credential> listAvailablesByWorkspaceId(Long workspaceId) {
        return credentialRepository.findActiveForWorkspaceFilterByPlatforms(workspaceId, accountPreferencesService.enabledPlatforms());
    }

    public Credential get(Long id, Workspace workspace) {
        return Optional.ofNullable(credentialRepository.findActiveByIdAndWorkspaceFilterByPlatforms(id, workspace.getId(),
                accountPreferencesService.enabledPlatforms())).orElseThrow(notFound(NOT_FOUND_FORMAT_MESS_ID, id));
    }

    public Map<String, String> interactiveLogin(Long workspaceId, Credential credential, Workspace workspace, User user) {
        credential.setWorkspace(workspace);
        return credentialAdapter.interactiveLogin(credential, workspaceId, user.getUserId());
    }

    public Credential updateByWorkspaceId(Long workspaceId, Credential credential, User user) {
        credentialValidator.validateCredentialCloudPlatform(credential.cloudPlatform());
        Credential original = Optional.ofNullable(
                credentialRepository.findActiveByNameAndWorkspaceIdFilterByPlatforms(credential.getName(), workspaceId,
                        accountPreferencesService.enabledPlatforms()))
                .orElseThrow(notFound(NOT_FOUND_FORMAT_MESS_NAME, credential.getName()));
        if (original.cloudPlatform() != null && !Objects.equals(credential.cloudPlatform(), original.cloudPlatform())) {
            throw new BadRequestException("Modifying credential platform is forbidden");
        }
        credential.setId(original.getId());
        credential.setWorkspace(workspaceService.get(workspaceId, user));
        Credential updated = super.create(credentialAdapter.init(credential, workspaceId, user.getUserId()), workspaceId, user);
        sendCredentialNotification(credential, ResourceEvent.CREDENTIAL_MODIFIED);
        return updated;
    }

    @Retryable(value = BadRequestException.class, maxAttempts = 30, backoff = @Backoff(delay = 2000))
    public void createWithRetry(Credential credential, Long workspaceId, User user) {
        create(credential, workspaceId, user);
    }

    @Override
    public Credential create(Credential credential, Long workspaceId, User user) {
        LOGGER.debug("Creating credential for workspace: {}", getWorkspaceService().get(workspaceId, user).getName());
        credentialValidator.validateCredentialCloudPlatform(credential.cloudPlatform());
        Credential created = super.create(credentialAdapter.init(credential, workspaceId, user.getUserId()), workspaceId, user);
        sendCredentialNotification(credential, ResourceEvent.CREDENTIAL_CREATED);
        return created;
    }

    public Credential delete(Long id, Workspace workspace) {
        Credential credential = Optional.ofNullable(
                credentialRepository.findActiveByIdAndWorkspaceFilterByPlatforms(id, workspace.getId(), accountPreferencesService.enabledPlatforms()))
                .orElseThrow(notFound(NOT_FOUND_FORMAT_MESS_ID, id));
        return delete(credential, workspace);
    }

    public Credential delete(String name, Workspace workspace) {
        Credential credential = Optional.ofNullable(
                credentialRepository.findActiveByNameAndWorkspaceIdFilterByPlatforms(name, workspace.getId(), accountPreferencesService.enabledPlatforms()))
                .orElseThrow(notFound(NOT_FOUND_FORMAT_MESS_NAME, name));
        return delete(credential, workspace);
    }

    public Set<CredentialResponse> convertAllToResponse(@Nonnull Iterable<Credential> credentials) {
        Set<CredentialResponse> jsonSet = new LinkedHashSet<>();
        for (Credential credential : credentials) {
            jsonSet.add(convertToResponse(credential));
        }
        return jsonSet;
    }

    public CredentialResponse convertToResponse(Credential credential) {
        return conversionService.convert(credential, CredentialResponse.class);
    }

    public Credential convertToCredential(CredentialRequest request) {
        return conversionService.convert(request, Credential.class);
    }

    public Set<Credential> findAllByCloudPlatform(String cloudPlatform) {
        return credentialRepository.findAllByCloudPlatform(cloudPlatform);
    }

    public void saveAllCredential(Iterable<Credential> credentials) {
        credentialRepository.saveAll(credentials);
    }

    public Set<Credential> findAllCredentialByTopology(Topology topology) {
        return credentialRepository.findByTopology(topology);
    }

    @Override
    public Credential deleteByNameFromWorkspace(String name, Long workspaceId) {
        Workspace workspace = getWorkspaceService().getByIdForCurrentUser(workspaceId);
        return delete(name, workspace);
    }

    @Override
    public Credential create(Credential resource, Workspace workspace, User user) {
        Credential created = super.create(resource, workspace, user);
        userProfileHandler.createProfilePreparation(created, user);
        return created;
    }

    @Override
    public WorkspaceResourceRepository<Credential, Long> repository() {
        return credentialRepository;
    }

    @Override
    public WorkspaceResource resource() {
        return WorkspaceResource.CREDENTIAL;
    }

    @Override
    protected void prepareCreation(Credential resource) {

    }

    public Credential archiveCredential(Credential credential) {
        credential.setName(generateArchiveName(credential.getName()));
        credential.setArchived(true);
        credential.setTopology(null);
        return credentialRepository.save(credential);
    }

    public CredentialPrerequisites getPrerequisites(User user, Workspace workspace, String cloudPlatform) {
        String cloudPlatformUppercased = cloudPlatform.toUpperCase();
        credentialValidator.validateCredentialCloudPlatform(cloudPlatformUppercased);
        return credentialPrerequisiteService.getPrerequisites(user, workspace, cloudPlatformUppercased);
    }

    @Override
    protected void prepareDeletion(Credential resource) {
        throw new UnsupportedOperationException("Credential deletion from database is not allowed, thus default deletion process is not supported!");
    }

    private Credential delete(Credential credential, Workspace workspace) {
        checkCredentialIsDeletable(credential);
        LOGGER.info(String.format("Starting to delete credential [name: %s, workspace: %s]", credential.getName(), workspace.getName()));
        userProfileHandler.destroyProfileCredentialPreparation(credential);
        Credential archived = archiveCredential(credential);
        sendCredentialNotification(credential, ResourceEvent.CREDENTIAL_DELETED);
        return archived;
    }

    private void checkCredentialIsDeletable(Credential credential) {
        LOGGER.info("Checking whether the desired credential is able to delete or not.");
        if (credential == null) {
            throw new NotFoundException("Credential not found.");
        }
        Set<Stack> stacksForCredential = stackRepository.findByCredential(credential);
        if (!stacksForCredential.isEmpty()) {
            String clusters;
            String message;
            if (stacksForCredential.size() > 1) {
                clusters = stacksForCredential.stream()
                        .map(Stack::getName)
                        .collect(Collectors.joining(", "));
                message = "There are clusters associated with credential config '%s'. Please remove these before deleting the credential. "
                        + "The following clusters are using this credential: [%s]";
            } else {
                clusters = stacksForCredential.iterator().next().getName();
                message = "There is a cluster associated with credential config '%s'. Please remove before deleting the credential. "
                        + "The following cluster is using this credential: [%s]";
            }
            throw new BadRequestException(String.format(message, credential.getName(), clusters));
        }
    }

    private void sendCredentialNotification(Credential credential, ResourceEvent resourceEvent) {
        CloudbreakEventsJson notification = new CloudbreakEventsJson();
        notification.setEventType(resourceEvent.name());
        notification.setEventTimestamp(new Date().getTime());
        notification.setEventMessage(messagesService.getMessage(resourceEvent.getMessage()));
        notification.setCloud(credential.cloudPlatform());
        notification.setWorkspaceId(credential.getWorkspace().getId());
        notificationSender.send(new Notification<>(notification));
    }
}