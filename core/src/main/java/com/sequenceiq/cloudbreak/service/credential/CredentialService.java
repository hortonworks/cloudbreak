package com.sequenceiq.cloudbreak.service.credential;

import static com.sequenceiq.cloudbreak.util.NameUtil.generateArchiveName;
import static com.sequenceiq.cloudbreak.util.SqlUtil.getProperSqlErrorMessage;

import java.util.Date;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.model.CloudbreakEventsJson;
import com.sequenceiq.cloudbreak.common.model.user.IdentityUser;
import com.sequenceiq.cloudbreak.common.model.user.IdentityUserRole;
import com.sequenceiq.cloudbreak.common.type.APIResourceType;
import com.sequenceiq.cloudbreak.common.type.ResourceEvent;
import com.sequenceiq.cloudbreak.controller.exception.BadRequestException;
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.domain.security.Organization;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.repository.CredentialRepository;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.service.AuthorizationService;
import com.sequenceiq.cloudbreak.service.account.AccountPreferencesService;
import com.sequenceiq.cloudbreak.service.messages.CloudbreakMessagesService;
import com.sequenceiq.cloudbreak.service.notification.Notification;
import com.sequenceiq.cloudbreak.service.notification.NotificationSender;
import com.sequenceiq.cloudbreak.service.organization.OrganizationService;
import com.sequenceiq.cloudbreak.service.stack.connector.adapter.ServiceProviderCredentialAdapter;
import com.sequenceiq.cloudbreak.service.user.UserProfileHandler;

@Service
public class CredentialService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CredentialService.class);

    @Inject
    private CredentialRepository credentialRepository;

    @Inject
    private StackRepository stackRepository;

    @Inject
    private ServiceProviderCredentialAdapter credentialAdapter;

    @Inject
    private AuthorizationService authorizationService;

    @Inject
    private UserProfileHandler userProfileHandler;

    @Inject
    private AccountPreferencesService accountPreferencesService;

    @Inject
    private NotificationSender notificationSender;

    @Inject
    private CloudbreakMessagesService messagesService;

    @Inject
    private OrganizationService organizationService;

    public Set<Credential> retrievePrivateCredentials(IdentityUser user) {
        return credentialRepository.findForUser(user.getUserId());
    }

    public Set<Credential> retrieveAccountCredentials(IdentityUser user) {
        Set<String> platforms = accountPreferencesService.enabledPlatforms();
        return user.getRoles().contains(IdentityUserRole.ADMIN)
                ? credentialRepository.findAllInAccountAndFilterByPlatforms(user.getAccount(), platforms)
                : credentialRepository.findPublicInAccountForUserFilterByPlatforms(user.getUserId(), user.getAccount(), platforms);
    }

    public Credential get(Long id) {
        return credentialRepository.findById(id)
                .orElseThrow(accessDenied(String.format("Access is denied: Credential not found by id '%d'.", id)));
    }

    public Supplier<AccessDeniedException> accessDenied(String accessDeniedMessage) {
        return () -> new AccessDeniedException(accessDeniedMessage);
    }

    public Credential get(Long id, String account) {
        return Optional.ofNullable(credentialRepository.findByIdInAccount(id, account))
                .orElseThrow(accessDenied(String.format("Access is denied: Credential not found by id '%d' in %s account.", id, account)));
    }

    public Credential get(String name, String account) {
        return Optional.ofNullable(credentialRepository.findOneByName(name, account))
                .orElseThrow(accessDenied(String.format("Access is denied: Credential not found by name '%s' in %s account.", name, account)));
    }

    public Map<String, String> interactiveLogin(IdentityUser user, Credential credential) {
        LOGGER.debug("Interactive login: [User: '{}', Account: '{}']", user.getUsername(), user.getAccount());
        credential.setOwner(user.getUserId());
        credential.setAccount(user.getAccount());
        return credentialAdapter.interactiveLogin(credential);
    }

    public Credential create(IdentityUser user, Credential credential) {
        LOGGER.debug("Creating credential: [User: '{}', Account: '{}']", user.getUsername(), user.getAccount());
        credential.setOwner(user.getUserId());
        credential.setAccount(user.getAccount());
        return saveCredentialAndNotify(credential, ResourceEvent.CREDENTIAL_CREATED);
    }

    public Credential modify(IdentityUser user, Credential credential) {
        LOGGER.debug("Modifying credential: [User: '{}', Account: '{}']", user.getUsername(), user.getAccount());
        Credential credentialToModify = credential.isPublicInAccount() ? getPublicCredential(credential.getName(), user)
                : getPrivateCredential(credential.getName(), user);
        if (!credentialToModify.cloudPlatform().equals(credential.cloudPlatform())) {
            throw new BadRequestException("Modifying credential platform is forbidden");
        }
        if (credential.getAttributes() != null) {
            credentialToModify.setAttributes(credential.getAttributes());
        }
        if (credential.getDescription() != null) {
            credentialToModify.setDescription(credential.getDescription());
        }
        if (credential.getTopology() != null) {
            credentialToModify.setTopology(credential.getTopology());
        }
        return saveCredentialAndNotify(credentialToModify, ResourceEvent.CREDENTIAL_MODIFIED);
    }

    public Credential create(String userId, String account, Credential credential, IdentityUser identityUser) {
        LOGGER.debug("Creating credential: [UserId: '{}', Account: '{}']", userId, account);
        credential.setOwner(userId);
        credential.setAccount(account);
        Organization organization = organizationService.getDefaultOrganizationForCurrentUser();
        credential.setOrganization(organization);
        return saveCredentialAndNotify(credential, ResourceEvent.CREDENTIAL_CREATED);
    }

    @Retryable(value = BadRequestException.class, maxAttempts = 30, backoff = @Backoff(delay = 2000))
    public Credential createWithRetry(String userId, String account, Credential credential, IdentityUser identityUser) {
        return create(userId, account, credential, identityUser);
    }

    private Credential saveCredentialAndNotify(Credential credential, ResourceEvent resourceEvent) {
        credential = credentialAdapter.init(credential);
        Credential savedCredential;
        try {
            savedCredential = credentialRepository.save(credential);
            userProfileHandler.createProfilePreparation(credential);
            sendCredentialNotification(credential, resourceEvent);
        } catch (DataIntegrityViolationException ex) {
            String msg = String.format("Error with resource [%s], %s", APIResourceType.CREDENTIAL, getProperSqlErrorMessage(ex));
            throw new BadRequestException(msg);
        }
        return savedCredential;
    }

    private void sendCredentialNotification(Credential credential, ResourceEvent resourceEvent) {
        CloudbreakEventsJson notification = new CloudbreakEventsJson();
        notification.setEventType(resourceEvent.name());
        notification.setEventTimestamp(new Date().getTime());
        notification.setEventMessage(messagesService.getMessage(resourceEvent.getMessage()));
        notification.setOwner(credential.getOwner());
        notification.setAccount(credential.getAccount());
        notification.setCloud(credential.cloudPlatform());
        notificationSender.send(new Notification<>(notification));
    }

    public Credential getPublicCredential(String name, IdentityUser user) {
        return Optional.ofNullable(credentialRepository.findOneByName(name, user.getAccount()))
                .orElseThrow(accessDenied(String.format("Access is denied: Credential not found by name '%s'", name)));
    }

    public Credential getPrivateCredential(String name, IdentityUser user) {
        return Optional.ofNullable(credentialRepository.findByNameInUser(name, user.getUserId()))
                .orElseThrow(accessDenied(String.format("Access is denied: Credential not found by name '%s'.", name)));
    }

    public void delete(Long id, IdentityUser user) {
        Credential credential = Optional.ofNullable(credentialRepository.findByIdInAccount(id, user.getAccount()))
                .orElseThrow(accessDenied(String.format("Access is denied: Credential not found by id: '%d'.", id)));
        delete(credential);
    }

    public void delete(String name, IdentityUser user) {
        Credential credential = Optional.ofNullable(credentialRepository.findByNameInAccount(name, user.getAccount(), user.getUserId()))
                .orElseThrow(accessDenied(String.format("Access is denied: Credential not found by name '%s'.", name)));
        delete(credential);
    }

    public Credential update(Long id) {
        return credentialAdapter.update(get(id));
    }

    private void delete(Credential credential) {
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
        userProfileHandler.destroyProfileCredentialPreparation(credential);
        archiveCredential(credential);
    }

    public void archiveCredential(Credential credential) {
        credential.setName(generateArchiveName(credential.getName()));
        credential.setArchived(true);
        credential.setTopology(null);
        credentialRepository.save(credential);
    }
}
