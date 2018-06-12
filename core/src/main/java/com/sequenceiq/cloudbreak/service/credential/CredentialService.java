package com.sequenceiq.cloudbreak.service.credential;

import com.sequenceiq.cloudbreak.api.model.CloudbreakEventsJson;
import com.sequenceiq.cloudbreak.common.model.user.IdentityUser;
import com.sequenceiq.cloudbreak.common.model.user.IdentityUserRole;
import com.sequenceiq.cloudbreak.common.type.APIResourceType;
import com.sequenceiq.cloudbreak.common.type.ResourceEvent;
import com.sequenceiq.cloudbreak.controller.exception.BadRequestException;
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.repository.CredentialRepository;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.service.AuthorizationService;
import com.sequenceiq.cloudbreak.service.account.AccountPreferencesService;
import com.sequenceiq.cloudbreak.service.messages.CloudbreakMessagesService;
import com.sequenceiq.cloudbreak.service.notification.Notification;
import com.sequenceiq.cloudbreak.service.notification.NotificationSender;
import com.sequenceiq.cloudbreak.service.stack.connector.adapter.ServiceProviderCredentialAdapter;
import com.sequenceiq.cloudbreak.service.user.UserProfileCredentialHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.Date;
import java.util.Map;
import java.util.Set;

import static com.sequenceiq.cloudbreak.util.NameUtil.generateArchiveName;
import static com.sequenceiq.cloudbreak.util.SqlUtil.getProperSqlErrorMessage;

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
    private UserProfileCredentialHandler userProfileCredentialHandler;

    @Inject
    private AccountPreferencesService accountPreferencesService;

    @Inject
    private NotificationSender notificationSender;

    @Inject
    private CloudbreakMessagesService messagesService;

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
        Credential credential = credentialRepository.findOne(id);
        if (credential == null) {
            throw new AccessDeniedException(String.format("Access is denied: Credential '%d'.", id));
        }
        authorizationService.hasReadPermission(credential);
        return credential;
    }

    public Credential get(Long id, String account) {
        Credential credential = credentialRepository.findByIdInAccount(id, account);
        if (credential == null) {
            throw new AccessDeniedException(String.format("Access is denied: Credential '%d' in %s account.", id, account));
        }
        authorizationService.hasReadPermission(credential);
        return credential;
    }

    public Credential get(String name, String account) {
        Credential credential = credentialRepository.findOneByName(name, account);
        if (credential == null) {
            throw new AccessDeniedException(String.format("Access is denied: Credential '%s' in %s account.", name, account));
        }
        authorizationService.hasReadPermission(credential);
        return credential;
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
        authorizationService.hasWritePermission(credentialToModify);
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

    public Credential create(String userId, String account, Credential credential) {
        LOGGER.debug("Creating credential: [UserId: '{}', Account: '{}']", userId, account);
        credential.setOwner(userId);
        credential.setAccount(account);
        return saveCredentialAndNotify(credential, ResourceEvent.CREDENTIAL_CREATED);
    }

    @Retryable(value = BadRequestException.class, maxAttempts = 30, backoff = @Backoff(delay = 2000))
    public Credential createWithRetry(String userId, String account, Credential credential) {
        return create(userId, account, credential);
    }

    private Credential saveCredentialAndNotify(Credential credential, ResourceEvent resourceEvent) {
        credential = credentialAdapter.init(credential);
        Credential savedCredential;
        try {
            savedCredential = credentialRepository.save(credential);
            userProfileCredentialHandler.createProfilePreparation(credential);
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
        Credential credential = credentialRepository.findOneByName(name, user.getAccount());
        if (credential == null) {
            throw new AccessDeniedException(String.format("Access is denied: Credential '%s'", name));
        }
        authorizationService.hasReadPermission(credential);
        return credential;
    }

    public Credential getPrivateCredential(String name, IdentityUser user) {
        Credential credential = credentialRepository.findByNameInUser(name, user.getUserId());
        if (credential == null) {
            throw new AccessDeniedException(String.format("Access is denied: Credential '%s'.", name));
        }
        authorizationService.hasReadPermission(credential);
        return credential;
    }

    public void delete(Long id, IdentityUser user) {
        Credential credential = credentialRepository.findByIdInAccount(id, user.getAccount());
        if (credential == null) {
            throw new AccessDeniedException(String.format("Access is denied: Credential '%d'.", id));
        }
        delete(credential);
    }

    public void delete(String name, IdentityUser user) {
        Credential credential = credentialRepository.findByNameInAccount(name, user.getAccount(), user.getUserId());
        if (credential == null) {
            throw new AccessDeniedException(String.format("Access is denied: Credential '%s'.", name));
        }
        delete(credential);
    }

    public Credential update(Long id) {
        return credentialAdapter.update(get(id));
    }

    private void delete(Credential credential) {
        authorizationService.hasWritePermission(credential);
        if (!stackRepository.countByCredential(credential).equals(0L)) {
            throw new BadRequestException(String.format("Credential '%d' is in use, cannot be deleted.", credential.getId()));
        }
        userProfileCredentialHandler.destroyProfilePreparation(credential);
        archiveCredential(credential);
    }

    public void archiveCredential(Credential credential) {
        credential.setName(generateArchiveName(credential.getName()));
        credential.setArchived(true);
        credential.setTopology(null);
        credentialRepository.save(credential);
    }
}
