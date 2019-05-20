package com.sequenceiq.environment.credential;


import static com.sequenceiq.cloudbreak.common.exception.NotFoundException.notFound;
import static com.sequenceiq.cloudbreak.util.NameUtil.generateArchiveName;
import static com.sequenceiq.environment.TempConstants.TEMP_ACCOUNT_ID;
import static com.sequenceiq.environment.TempConstants.TEMP_USER_ID;
import static com.sequenceiq.environment.TempConstants.TEMP_WORKSPACE_ID;

import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotFoundException;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.client.RestClientUtil;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.response.CredentialPrerequisitesResponse;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.message.CloudbreakMessagesService;
import com.sequenceiq.cloudbreak.structuredevent.event.CloudbreakNotification;
import com.sequenceiq.environment.credential.exception.CredentialOperationException;
import com.sequenceiq.environment.credential.validation.CredentialValidator;
import com.sequenceiq.environment.environment.domain.EnvironmentView;
import com.sequenceiq.environment.environment.service.EnvironmentViewService;
import com.sequenceiq.notification.Notification;
import com.sequenceiq.notification.NotificationSender;
import com.sequenceiq.notification.ResourceEvent;
import com.sequenceiq.cloudbreak.service.secret.service.SecretService;

@Service
public class CredentialService {

    static final String DEPLOYMENT_ADDRESS_ATTRIBUTE_NOT_FOUND = "The 'deploymentAddress' parameter needs to be specified in the interactive login request!";

    private static final Logger LOGGER = LoggerFactory.getLogger(CredentialService.class);

    private static final String NOT_FOUND_FORMAT_MESS_ID = "Credential with id:";

    private static final String NOT_FOUND_FORMAT_MESS_NAME = "Credential with name:";

    private static final Set<String> ENABLED_PLATFORMS = Set.of("AWS", "AZURE");

    @Inject
    private CredentialRepository repository;

    @Inject
    private CredentialValidator credentialValidator;

    @Inject
    private ServiceProviderCredentialAdapter credentialAdapter;

    @Inject
    private NotificationSender notificationSender;

    @Inject
    private CloudbreakMessagesService messagesService;

    @Inject
    private CredentialPrerequisiteService credentialPrerequisiteService;

    @Inject
    private EnvironmentViewService environmentViewService;

    @Inject
    private SecretService secretService;

    public Set<Credential> listAvailablesByWorkspaceId(Long workspaceId) {
        return new HashSet<>(repository.findAll());
    }

    public Credential get(Long id, Long workspaceId) {
        return repository.findActiveByIdAndAccountFilterByPlatforms(id, TEMP_ACCOUNT_ID, ENABLED_PLATFORMS).get();
    }

    public Credential getByNameForAccountId(String name, Long workspaceId) {
        return repository.findActiveByNameAndAccountIdFilterByPlatforms(name, TEMP_ACCOUNT_ID, ENABLED_PLATFORMS).get();
    }

    public Map<String, String> interactiveLogin(Long workspaceId, Credential credential) {
        validateDeploymentAddress(credential);
        return credentialAdapter.interactiveLogin(credential, workspaceId, "0");
    }

    public Credential updateByWorkspaceId(Long workspaceId, Credential credential) {
        Credential original = repository.findActiveByNameAndAccountIdFilterByPlatforms(credential.getName(), TEMP_ACCOUNT_ID,
                ENABLED_PLATFORMS).orElseThrow(notFound(NOT_FOUND_FORMAT_MESS_NAME, credential.getName()));
        if (!Objects.equals(credential.getCloudPlatform(), original.getCloudPlatform())) {
            throw new BadRequestException("Modifying credential platform is forbidden");
        }
        credential.setId(original.getId());
        credential.setAccountId(workspaceId.toString());
        Credential updated = repository.save(credentialAdapter.verify(credential, workspaceId));
        secretService.delete(original.getAttributesSecret());
        sendCredentialNotification(credential, ResourceEvent.CREDENTIAL_MODIFIED);
        return updated;
    }

    @Retryable(value = BadRequestException.class, maxAttempts = 30, backoff = @Backoff(delay = 2000))
    public void createWithRetry(Credential credential, Long workspaceId) {
        create(credential, workspaceId);
    }

    public Credential create(Credential credential, @Nonnull Long workspaceId) {
        credentialValidator.validateCredentialCloudPlatform(credential.getCloudPlatform());
        credentialValidator.validateParameters(Platform.platform(credential.getCloudPlatform()), new Json(credential.getAttributes()).getMap());
        Credential created = repository.save(credentialAdapter.verify(credential, TEMP_WORKSPACE_ID));
        sendCredentialNotification(credential, ResourceEvent.CREDENTIAL_CREATED);
        return created;
    }

    public Credential delete(Long id, String accountId) {
        Credential credential = repository.findActiveByIdAndAccountFilterByPlatforms(id, TEMP_ACCOUNT_ID, ENABLED_PLATFORMS)
                .orElseThrow(notFound(NOT_FOUND_FORMAT_MESS_ID, id));
        return delete(credential);
    }

    public Credential delete(String name, String accountId) {
        Credential credential = repository.findActiveByNameAndAccountIdFilterByPlatforms(name, TEMP_ACCOUNT_ID,
                ENABLED_PLATFORMS).orElseThrow(notFound(NOT_FOUND_FORMAT_MESS_NAME, name));
        return delete(credential);
    }

    public Credential deleteByNameFromWorkspace(String name, Long workspaceId) {
        return delete(name, TEMP_USER_ID);
    }

    public Credential archiveCredential(Credential credential) {
        credential.setName(generateArchiveName(credential.getName()));
        credential.setArchived(true);
        return repository.save(credential);
    }

    public CredentialPrerequisitesResponse getPrerequisites(Long workspaceId, String cloudPlatform, String deploymentAddress) {
        String cloudPlatformUppercased = cloudPlatform.toUpperCase();
        credentialValidator.validateCredentialCloudPlatform(cloudPlatformUppercased);
        return credentialPrerequisiteService.getPrerequisites(cloudPlatformUppercased, deploymentAddress);
    }

    public String initCodeGrantFlow(Long workspaceId, @Nonnull Credential credential) {
        credentialValidator.validateCredentialCloudPlatform(credential.getCloudPlatform());
        validateDeploymentAddress(credential);
        putToCredentialAttributes(credential, Map.of("codeGrantFlow", true));
        Credential created = credentialAdapter.initCodeGrantFlow(credential, workspaceId, TEMP_USER_ID);
        created = repository.save(created);
        return getCodeGrantFlowAppLoginUrl(created.getAttributes());
    }

    public String initCodeGrantFlow(Long workspaceId, String name) {
        Credential original = repository.findActiveByNameAndAccountIdFilterByPlatforms(name, TEMP_ACCOUNT_ID, ENABLED_PLATFORMS)
                .orElseThrow(notFound(NOT_FOUND_FORMAT_MESS_NAME, name));
        String originalAttributes = original.getAttributes();
        boolean codeGrantFlow = Boolean.valueOf(new Json(originalAttributes).getMap().get("codeGrantFlow").toString());
        if (!codeGrantFlow) {
            throw new UnsupportedOperationException("This operation is only allowed on Authorization Code Grant flow based credentails.");
        }
        Credential updated = credentialAdapter.initCodeGrantFlow(original, workspaceId, TEMP_USER_ID);
        updated = repository.save(updated);
        secretService.delete(originalAttributes);
        return getCodeGrantFlowAppLoginUrl(updated.getAttributes());
    }

    public Credential authorizeCodeGrantFlow(String code, @Nonnull String state, Long workspaceId, @Nonnull String platform) {
        String cloudPlatformUpperCased = platform.toUpperCase();
        Set<Credential> credentials = repository.findActiveForAccountFilterByPlatforms(TEMP_ACCOUNT_ID, List.of(cloudPlatformUpperCased));
        Credential original = credentials.stream()
                .filter(cred -> state.equalsIgnoreCase(String.valueOf(new Json(cred.getAttributes()).getMap().get("codeGrantFlowState"))))
                .findFirst()
                .orElseThrow(notFound("Code grant flow based credential for user with state:", state));
        LOGGER.info("Authorizing credential('{}') with Authorization Code Grant flow.", original.getName());
        String attributesSecret = original.getAttributesSecret();
        putToCredentialAttributes(original, Map.of("authorizationCode", code));
        Credential updated = repository.save(credentialAdapter.verify(original, workspaceId));
        secretService.delete(attributesSecret);
        return updated;
    }

    private void validateDeploymentAddress(Credential credential) {
        String deploymentAddress = (String) new Json(credential.getAttributes()).getMap().get("deploymentAddress");
        if (StringUtils.isEmpty(deploymentAddress)) {
            throw new BadRequestException(DEPLOYMENT_ADDRESS_ATTRIBUTE_NOT_FOUND);
        }
    }

    private Credential delete(Credential credential) {
        checkCredentialIsDeletable(credential);
        Credential archived = archiveCredential(credential);
        sendCredentialNotification(credential, ResourceEvent.CREDENTIAL_DELETED);
        return archived;
    }

    private void checkCredentialIsDeletable(Credential credential) {
        LOGGER.debug("Checking whether the desired credential is able to delete or not.");
        if (credential == null) {
            throw new NotFoundException("Credential not found.");
        }
        checkEnvironmentsForDeletion(credential);
    }

    private void checkEnvironmentsForDeletion(Credential credential) {
        Set<EnvironmentView> environments = environmentViewService.findAllByCredentialId(credential.getId());
        if (!environments.isEmpty()) {
            String environmentList = environments.stream().map(EnvironmentView::getName).collect(Collectors.joining(", "));
            String message = "Credential '%s' cannot be deleted because the following environments are using it: [%s].";
            throw new BadRequestException(String.format(message, credential.getName(), environmentList));
        }
    }

    private void sendCredentialNotification(Credential credential, ResourceEvent resourceEvent) {
        CloudbreakNotification notification = new CloudbreakNotification();
        notification.setEventType(resourceEvent.name());
        notification.setEventTimestamp(new Date().getTime());
        notification.setEventMessage(messagesService.getMessage(resourceEvent.getMessage()));
        notification.setCloud(credential.getCloudPlatform());
        notificationSender.send(new Notification<>(notification), Collections.emptyList(), RestClientUtil.get());
    }

    private void putToCredentialAttributes(Credential credential, Map<String, Object> attributesToAdd) {
        Json attributes = new Json(credential.getAttributes());
        Map<String, Object> newAttributes = attributes.getMap();
        newAttributes.putAll(attributesToAdd);
        credential.setAttributes(new Json(newAttributes).getValue());
    }

    private String getCodeGrantFlowAppLoginUrl(String credentialAttributes) {
        Object appLoginUrl = Optional.ofNullable(new Json(credentialAttributes).getMap().get("appLoginUrl"))
                .orElseThrow(() -> new CredentialOperationException("Unable to obtain App login url!"));
        return String.valueOf(appLoginUrl);
    }
}