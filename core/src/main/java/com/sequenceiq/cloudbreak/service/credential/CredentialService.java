package com.sequenceiq.cloudbreak.service.credential;

import static com.sequenceiq.cloudbreak.util.SqlUtil.getProperSqlErrorMessage;

import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.model.CloudbreakEventsJson;
import com.sequenceiq.cloudbreak.common.model.user.IdentityUser;
import com.sequenceiq.cloudbreak.common.model.user.IdentityUserRole;
import com.sequenceiq.cloudbreak.common.type.APIResourceType;
import com.sequenceiq.cloudbreak.controller.BadRequestException;
import com.sequenceiq.cloudbreak.controller.NotFoundException;
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.repository.CredentialRepository;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.service.AuthorizationService;
import com.sequenceiq.cloudbreak.service.notification.Notification;
import com.sequenceiq.cloudbreak.service.notification.NotificationSender;
import com.sequenceiq.cloudbreak.service.stack.connector.adapter.ServiceProviderCredentialAdapter;
import com.sequenceiq.cloudbreak.service.user.UserProfileCredentialHandler;

@Service
@Transactional
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
    private NotificationSender notificationSender;

    public Set<Credential> retrievePrivateCredentials(IdentityUser user) {
        return credentialRepository.findForUser(user.getUserId());
    }

    public Set<Credential> retrieveAccountCredentials(IdentityUser user) {
        if (user.getRoles().contains(IdentityUserRole.ADMIN)) {
            return credentialRepository.findAllInAccount(user.getAccount());
        } else {
            return credentialRepository.findPublicInAccountForUser(user.getUserId(), user.getAccount());
        }
    }

    public Credential get(Long id) {
        Credential credential = credentialRepository.findOne(id);
        if (credential == null) {
            throw new NotFoundException(String.format("Credential '%s' not found.", id));
        }
        authorizationService.hasReadPermission(credential);
        return credential;
    }

    public Credential get(String name, String account) {
        Credential credential = credentialRepository.findOneByName(name, account);
        if (credential == null) {
            throw new NotFoundException(String.format("Credential '%s' not found in %s account.", name, account));
        }
        authorizationService.hasReadPermission(credential);
        return credential;
    }

    @Transactional(TxType.NEVER)
    public Map<String, String> interactiveLogin(IdentityUser user, Credential credential) {
        LOGGER.debug("Interactive login: [User: '{}', Account: '{}']", user.getUsername(), user.getAccount());
        credential.setOwner(user.getUserId());
        credential.setAccount(user.getAccount());
        return credentialAdapter.interactiveLogin(credential);
    }

    @Transactional(TxType.NEVER)
    public Credential create(IdentityUser user, Credential credential) {
        LOGGER.debug("Creating credential: [User: '{}', Account: '{}']", user.getUsername(), user.getAccount());
        credential.setOwner(user.getUserId());
        credential.setAccount(user.getAccount());
        return saveCredential(credential);
    }

    @Transactional(TxType.NEVER)
    public Credential create(String userId, String account, Credential credential) {
        LOGGER.debug("Creating credential: [UserId: '{}', Account: '{}']", userId, account);
        credential.setOwner(userId);
        credential.setAccount(account);
        return saveCredential(credential);
    }

    @Transactional(TxType.NEVER)
    @Retryable(value = BadRequestException.class, maxAttempts = 30, backoff = @Backoff(delay = 2000))
    public Credential createWithRetry(String userId, String account, Credential credential) {
        return create(userId, account, credential);
    }

    private Credential saveCredential(Credential credential) {
        credential = credentialAdapter.init(credential);
        Credential savedCredential;
        try {
            savedCredential = credentialRepository.save(credential);
            userProfileCredentialHandler.createProfilePreparation(credential);
            sendCredentialCreatedNotification(credential);
        } catch (DataIntegrityViolationException ex) {
            String msg = String.format("Error with resource [%s], error: [%s]", APIResourceType.CREDENTIAL, getProperSqlErrorMessage(ex));
            throw new BadRequestException(msg);
        }
        return savedCredential;
    }

    private void sendCredentialCreatedNotification(Credential credential) {
        CloudbreakEventsJson notification = new CloudbreakEventsJson();
        notification.setEventType("CREDENTIAL_CREATED");
        notification.setEventTimestamp(new Date().getTime());
        notification.setEventMessage("Credential created");
        notification.setOwner(credential.getOwner());
        notification.setAccount(credential.getAccount());
        notification.setCloud(credential.cloudPlatform());
        notificationSender.send(new Notification<>(notification));
    }

    public Credential getPublicCredential(String name, IdentityUser user) {
        Credential credential = credentialRepository.findOneByName(name, user.getAccount());
        if (credential == null) {
            throw new NotFoundException(String.format("Credential '%s' not found.", name));
        } else {
            return credential;
        }
    }

    public Credential getPrivateCredential(String name, IdentityUser user) {
        Credential credential = credentialRepository.findByNameInUser(name, user.getUserId());
        if (credential == null) {
            throw new NotFoundException(String.format("Credential '%s' not found.", name));
        } else {
            return credential;
        }
    }

    @Transactional(TxType.NEVER)
    public void delete(Long id, IdentityUser user) {
        Credential credential = credentialRepository.findByIdInAccount(id, user.getAccount());
        if (credential == null) {
            throw new NotFoundException(String.format("Credential '%s' not found.", id));
        }
        delete(credential);
    }

    @Transactional(TxType.NEVER)
    public void delete(String name, IdentityUser user) {
        Credential credential = credentialRepository.findByNameInAccount(name, user.getAccount(), user.getUserId());
        if (credential == null) {
            throw new NotFoundException(String.format("Credential '%s' not found.", name));
        }
        delete(credential);
    }

    @Transactional(TxType.NEVER)
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

    private String generateArchiveName(String name) {
        //generate new name for the archived credential to by pass unique constraint
        return new StringBuilder().append(name).append('_').append(UUID.randomUUID()).toString();
    }
}
