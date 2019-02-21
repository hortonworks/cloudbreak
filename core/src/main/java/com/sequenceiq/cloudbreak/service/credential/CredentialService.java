package com.sequenceiq.cloudbreak.service.credential;

import static com.sequenceiq.cloudbreak.controller.exception.NotFoundException.notFound;
import static com.sequenceiq.cloudbreak.util.NameUtil.generateArchiveName;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.responses.CredentialPrerequisitesV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.events.responses.CloudbreakEventV4Response;
import com.sequenceiq.cloudbreak.authorization.WorkspaceResource;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.common.type.ResourceEvent;
import com.sequenceiq.cloudbreak.controller.exception.BadRequestException;
import com.sequenceiq.cloudbreak.controller.exception.NotFoundException;
import com.sequenceiq.cloudbreak.controller.validation.credential.CredentialValidator;
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.domain.json.Json;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.view.EnvironmentView;
import com.sequenceiq.cloudbreak.domain.workspace.User;
import com.sequenceiq.cloudbreak.domain.workspace.Workspace;
import com.sequenceiq.cloudbreak.repository.CredentialRepository;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.repository.environment.EnvironmentViewRepository;
import com.sequenceiq.cloudbreak.repository.workspace.WorkspaceResourceRepository;
import com.sequenceiq.cloudbreak.service.AbstractWorkspaceAwareResourceService;
import com.sequenceiq.cloudbreak.service.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.service.account.PreferencesService;
import com.sequenceiq.cloudbreak.message.CloudbreakMessagesService;
import com.sequenceiq.cloudbreak.notification.Notification;
import com.sequenceiq.cloudbreak.notification.NotificationSender;
import com.sequenceiq.cloudbreak.service.secret.SecretService;
import com.sequenceiq.cloudbreak.service.stack.connector.adapter.ServiceProviderCredentialAdapter;
import com.sequenceiq.cloudbreak.service.user.UserProfileHandler;
import com.sequenceiq.cloudbreak.service.workspace.WorkspaceService;

@Service
public class CredentialService extends AbstractWorkspaceAwareResourceService<Credential> {

    static final String DEPLOYMENT_ADDRESS_ATTRIBUTE_NOT_FOUND = "The 'deploymentAddress' parameter needs to be specified in the interactive login request!";

    private static final Logger LOGGER = LoggerFactory.getLogger(CredentialService.class);

    private static final String NOT_FOUND_FORMAT_MESS_ID = "Credential with id:";

    private static final String NOT_FOUND_FORMAT_MESS_NAME = "Credential with name:";

    @Inject
    private CredentialRepository credentialRepository;

    @Inject
    private StackRepository stackRepository;

    @Inject
    private ServiceProviderCredentialAdapter credentialAdapter;

    @Inject
    private UserProfileHandler userProfileHandler;

    @Inject
    private PreferencesService preferencesService;

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

    @Inject
    private EnvironmentViewRepository environmentViewRepository;

    @Inject
    private SecretService secretService;

    public Set<Credential> listAvailablesByWorkspaceId(Long workspaceId) {
        return credentialRepository.findActiveForWorkspaceFilterByPlatforms(workspaceId, preferencesService.enabledPlatforms());
    }

    public Credential get(Long id, Workspace workspace) {
        return Optional.ofNullable(credentialRepository.findActiveByIdAndWorkspaceFilterByPlatforms(id, workspace.getId(),
                preferencesService.enabledPlatforms())).orElseThrow(notFound(NOT_FOUND_FORMAT_MESS_ID, id));
    }

    public Map<String, String> interactiveLogin(Long workspaceId, Credential credential) {
        User user = getLoggedInUser();
        Workspace workspace = getWorkspaceService().get(workspaceId, user);
        validateDeploymentAddress(credential);
        credential.setWorkspace(workspace);
        return credentialAdapter.interactiveLogin(credential, workspaceId, user.getUserId());
    }

    public Credential updateByWorkspaceId(Long workspaceId, Credential credential) {
        User user = getLoggedInUser();
        credentialValidator.validateCredentialCloudPlatform(credential.cloudPlatform());
        Credential original = Optional.ofNullable(
                credentialRepository.findActiveByNameAndWorkspaceIdFilterByPlatforms(credential.getName(), workspaceId,
                        preferencesService.enabledPlatforms()))
                .orElseThrow(notFound(NOT_FOUND_FORMAT_MESS_NAME, credential.getName()));
        if (!Objects.equals(credential.cloudPlatform(), original.cloudPlatform())) {
            throw new BadRequestException("Modifying credential platform is forbidden");
        }
        credential.setId(original.getId());
        credential.setWorkspace(workspaceService.get(workspaceId, user));
        Credential updated = super.create(credentialAdapter.verify(credential, workspaceId, user.getUserId()), workspaceId, user);
        secretService.delete(original.getAttributesSecret());
        sendCredentialNotification(credential, ResourceEvent.CREDENTIAL_MODIFIED);
        return updated;
    }

    @Retryable(value = BadRequestException.class, maxAttempts = 30, backoff = @Backoff(delay = 2000))
    public void createWithRetry(Credential credential, Long workspaceId, User user) {
        create(credential, workspaceId, user);
    }

    @Override
    public Credential create(Credential credential, @Nonnull Long workspaceId, User user) {
        LOGGER.debug("Creating credential for workspace: {}", getWorkspaceService().get(workspaceId, user).getName());
        credentialValidator.validateCredentialCloudPlatform(credential.cloudPlatform());
        credentialValidator.validateParameters(Platform.platform(credential.cloudPlatform()), new Json(credential.getAttributes()).getMap());
        Credential created = super.create(credentialAdapter.verify(credential, workspaceId, user.getUserId()), workspaceId, user);
        sendCredentialNotification(credential, ResourceEvent.CREDENTIAL_CREATED);
        return created;
    }

    public Credential delete(Long id, Workspace workspace) {
        Credential credential = Optional.ofNullable(
                credentialRepository.findActiveByIdAndWorkspaceFilterByPlatforms(id, workspace.getId(), preferencesService.enabledPlatforms()))
                .orElseThrow(notFound(NOT_FOUND_FORMAT_MESS_ID, id));
        return delete(credential, workspace);
    }

    public Credential delete(String name, Workspace workspace) {
        Credential credential = Optional.ofNullable(
                credentialRepository.findActiveByNameAndWorkspaceIdFilterByPlatforms(name, workspace.getId(), preferencesService.enabledPlatforms()))
                .orElseThrow(notFound(NOT_FOUND_FORMAT_MESS_NAME, name));
        return delete(credential, workspace);
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

    public CredentialPrerequisitesV4Response getPrerequisites(Long workspaceId, String cloudPlatform, String deploymentAddress) {
        User user = getLoggedInUser();
        Workspace workspace = getWorkspaceService().get(workspaceId, user);
        String cloudPlatformUppercased = cloudPlatform.toUpperCase();
        credentialValidator.validateCredentialCloudPlatform(cloudPlatformUppercased);
        return credentialPrerequisiteService.getPrerequisites(user, workspace, cloudPlatformUppercased, deploymentAddress);
    }

    public String initCodeGrantFlow(Long workspaceId, @Nonnull Credential credential) {
        User user = getLoggedInUser();
        return initCodeGrantFlow(workspaceId, credential, user);
    }

    public String initCodeGrantFlow(Long workspaceId, @Nonnull Credential credential, @Nonnull User user) {
        LOGGER.info("Initializing credential('{}') with Authorization Code Grant flow for workspace: {}", credential.getName(),
                getWorkspaceService().get(workspaceId, user).getName());
        credentialValidator.validateCredentialCloudPlatform(credential.cloudPlatform());
        validateDeploymentAddress(credential);
        putToCredentialAttributes(credential, Map.of("codeGrantFlow", true));
        Credential created = credentialAdapter.initCodeGrantFlow(credential, workspaceId, user.getUserId());
        created = super.create(created, workspaceId, user);
        return getCodeGrantFlowAppLoginUrl(created.getAttributes());
    }

    public String initCodeGrantFlow(Long workspaceId, String name) {
        User user = getLoggedInUser();
        Credential original = Optional.ofNullable(credentialRepository
                .findActiveByNameAndWorkspaceIdFilterByPlatforms(name, workspaceId, preferencesService.enabledPlatforms()))
                .orElseThrow(notFound(NOT_FOUND_FORMAT_MESS_NAME, name));
        String originalAttributes = original.getAttributes();
        boolean codeGrantFlow = Boolean.valueOf(new Json(originalAttributes).getMap().get("codeGrantFlow").toString());
        if (!codeGrantFlow) {
            throw new UnsupportedOperationException("This operation is only allowed on Authorization Code Grant flow based credentails.");
        }
        LOGGER.info("Reinitializing Authorization Code Grant flow on credential('{}') in workspace: {}", original.getName(),
                getWorkspaceService().get(workspaceId, user).getName());
        Credential updated = credentialAdapter.initCodeGrantFlow(original, workspaceId, user.getUserId());
        updated = super.create(updated, workspaceId, user);
        secretService.delete(originalAttributes);
        return getCodeGrantFlowAppLoginUrl(updated.getAttributes());
    }

    public Credential authorizeCodeGrantFlow(String code, @Nonnull String state, Long workspaceId, @Nonnull String platform) {
        User user = getLoggedInUser();
        String cloudPlatformUppercased = platform.toUpperCase();
        credentialValidator.validateCredentialCloudPlatform(cloudPlatformUppercased);
        Set<Credential> credentials = credentialRepository.findActiveForWorkspaceFilterByPlatforms(workspaceId, List.of(cloudPlatformUppercased));
        Credential original = credentials.stream()
                .filter(cred -> state.equalsIgnoreCase(String.valueOf(new Json(cred.getAttributes()).getMap().get("codeGrantFlowState"))))
                .findFirst()
                .orElseThrow(notFound("Code grant flow based credential for user with state:", state));
        LOGGER.info("Authorizing credential('{}') with Authorization Code Grant flow for workspace: {}", original.getName(),
                getWorkspaceService().get(workspaceId, user).getName());
        String attributesSecret = original.getAttributesSecret();
        putToCredentialAttributes(original, Map.of("authorizationCode", code));
        Credential updated = super.create(credentialAdapter.verify(original, workspaceId, user.getUserId()), workspaceId, user);
        secretService.delete(attributesSecret);
        return updated;
    }

    @Override
    protected void prepareDeletion(Credential resource) {
        throw new UnsupportedOperationException("Credential deletion from database is not allowed, thus default deletion process is not supported!");
    }

    private void validateDeploymentAddress(Credential credential) {
        String deploymentAddress = (String) new Json(credential.getAttributes()).getMap().get("deploymentAddress");
        if (StringUtils.isEmpty(deploymentAddress)) {
            throw new BadRequestException(DEPLOYMENT_ADDRESS_ATTRIBUTE_NOT_FOUND);
        }
    }

    private Credential delete(Credential credential, Workspace workspace) {
        checkCredentialIsDeletable(credential);
        LOGGER.debug(String.format("Starting to delete credential [name: %s, workspace: %s]", credential.getName(), workspace.getName()));
        userProfileHandler.destroyProfileCredentialPreparation(credential);
        Credential archived = archiveCredential(credential);
        sendCredentialNotification(credential, ResourceEvent.CREDENTIAL_DELETED);
        return archived;
    }

    private void checkCredentialIsDeletable(Credential credential) {
        LOGGER.debug("Checking whether the desired credential is able to delete or not.");
        if (credential == null) {
            throw new NotFoundException("Credential not found.");
        }
        checkStacksForDeletion(credential);
        checkEnvironmentsForDeletion(credential);
    }

    private void checkStacksForDeletion(Credential credential) {
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

    private void checkEnvironmentsForDeletion(Credential credential) {
        Set<EnvironmentView> environments = environmentViewRepository.findAllByCredentialId(credential.getId());
        if (!environments.isEmpty()) {
            String environmentList = environments.stream().map(EnvironmentView::getName).collect(Collectors.joining(", "));
            String message = "Credential '%s' cannot be deleted because the following environments are using it: [%s].";
            throw new BadRequestException(String.format(message, credential.getName(), environmentList));
        }
    }

    private void sendCredentialNotification(Credential credential, ResourceEvent resourceEvent) {
        CloudbreakEventV4Response notification = new CloudbreakEventV4Response();
        notification.setEventType(resourceEvent.name());
        notification.setEventTimestamp(new Date().getTime());
        notification.setEventMessage(messagesService.getMessage(resourceEvent.getMessage()));
        notification.setCloud(credential.cloudPlatform());
        notification.setWorkspaceId(credential.getWorkspace().getId());
        notificationSender.send(new Notification<>(notification));
    }

    private void putToCredentialAttributes(Credential credential, Map<String, Object> attributesToAdd) {
        try {
            Json attributes = new Json(credential.getAttributes());
            Map<String, Object> newAttributes = attributes.getMap();
            newAttributes.putAll(attributesToAdd);
            credential.setAttributes(new Json(newAttributes).getValue());
        } catch (IOException e) {
            String msg = "Credential's attributes couldn't be updated.";
            LOGGER.warn(msg, e);
            throw new CloudbreakServiceException(msg, e);
        }
    }

    private String getCodeGrantFlowAppLoginUrl(String credentialAttributes) {
        Object appLoginUrl = Optional.ofNullable(new Json(credentialAttributes).getMap().get("appLoginUrl"))
                .orElseThrow(() -> new CloudbreakServiceException("Unable to obtain App login url!"));
        return String.valueOf(appLoginUrl);
    }
}