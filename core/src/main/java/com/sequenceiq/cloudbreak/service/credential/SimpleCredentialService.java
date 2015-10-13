package com.sequenceiq.cloudbreak.service.credential;

import java.util.Calendar;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.type.APIResourceType;
import com.sequenceiq.cloudbreak.common.type.CbUserRole;
import com.sequenceiq.cloudbreak.common.type.Status;
import com.sequenceiq.cloudbreak.controller.BadRequestException;
import com.sequenceiq.cloudbreak.controller.NotFoundException;
import com.sequenceiq.cloudbreak.domain.CbUser;
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.repository.CredentialRepository;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.service.CloudPlatformResolver;
import com.sequenceiq.cloudbreak.service.DuplicateKeyValueException;
import com.sequenceiq.cloudbreak.service.notification.Notification;
import com.sequenceiq.cloudbreak.service.notification.NotificationSender;
import com.sequenceiq.cloudbreak.service.stack.connector.OperationException;

@Service
public class SimpleCredentialService implements CredentialService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleCredentialService.class);

    @Inject
    private CredentialRepository credentialRepository;

    @Inject
    private StackRepository stackRepository;

    @Inject
    private CloudPlatformResolver platformResolver;

    @Inject
    private NotificationSender notificationSender;

    @Override
    public Set<Credential> retrievePrivateCredentials(CbUser user) {
        return credentialRepository.findForUser(user.getUserId());
    }

    @Override
    public Set<Credential> retrieveAccountCredentials(CbUser user) {
        if (user.getRoles().contains(CbUserRole.ADMIN)) {
            return credentialRepository.findAllInAccount(user.getAccount());
        } else {
            return credentialRepository.findPublicInAccountForUser(user.getUserId(), user.getAccount());
        }
    }

    @Override
    @PostAuthorize("hasPermission(returnObject,'read')")
    public Credential get(Long id) {
        Credential credential = credentialRepository.findOne(id);
        if (credential == null) {
            throw new NotFoundException(String.format("Credential '%s' not found.", id));
        } else {
            return credential;
        }
    }

    @Override
    public Credential create(CbUser user, Credential credential) {
        LOGGER.debug("Creating credential: [User: '{}', Account: '{}']", user.getUsername(), user.getAccount());
        credential.setOwner(user.getUserId());
        credential.setAccount(user.getAccount());
        Credential savedCredential;
        try {
            savedCredential = credentialRepository.save(credential);
        } catch (DataIntegrityViolationException ex) {
            throw new DuplicateKeyValueException(APIResourceType.CREDENTIAL, credential.getName(), ex);
        }
        try {
            platformResolver.credential(credential.cloudPlatform()).init(credential);
        } catch (Exception e) {
            credentialRepository.delete(savedCredential);
            throw e;
        }
        return savedCredential;
    }

    @Override
    public Credential getPublicCredential(String name, CbUser user) {
        Credential credential = credentialRepository.findOneByName(name, user.getAccount());
        if (credential == null) {
            throw new NotFoundException(String.format("Credential '%s' not found.", name));
        } else {
            return credential;
        }
    }

    @Override
    public Credential getPrivateCredential(String name, CbUser user) {
        Credential credential = credentialRepository.findByNameInUser(name, user.getUserId());
        if (credential == null) {
            throw new NotFoundException(String.format("Credential '%s' not found.", name));
        } else {
            return credential;
        }
    }

    @Override
    public void delete(Long id, CbUser user) {
        Credential credential = credentialRepository.findByIdInAccount(id, user.getAccount());
        if (credential == null) {
            throw new NotFoundException(String.format("Credential '%s' not found.", id));
        }
        delete(credential, user);
    }

    @Override
    public void delete(String name, CbUser user) {
        Credential credential = credentialRepository.findByNameInAccount(name, user.getAccount(), user.getUserId());
        if (credential == null) {
            throw new NotFoundException(String.format("Credential '%s' not found.", name));
        }
        delete(credential, user);
    }

    @Override
    public Credential update(Long id) throws Exception {
        Credential credential = get(id);
        if (credential == null) {
            throw new NotFoundException(String.format("Credential '%s' not found.", id));
        } else {
            return platformResolver.credential(credential.cloudPlatform()).update(credential);
        }
    }

    private void delete(Credential credential, CbUser user) {
        if (!user.getUserId().equals(credential.getOwner()) && !user.getRoles().contains(CbUserRole.ADMIN)) {
            throw new BadRequestException("Credentials can be deleted only by account admins or owners.");
        }
        List<Stack> stacks = stackRepository.findByCredential(credential.getId());
        if (stacks.isEmpty()) {
            try {
                platformResolver.credential(credential.cloudPlatform()).delete(credential);
            } catch (OperationException e) {
                LOGGER.error("Error during deleting cloud provider credential. Archiving local credential.", e);
                notificationSender.send(getCredentialNotification(credential, Status.DELETE_FAILED.name(),
                        "Error during deleting cloud provider credential. Please delete the cloud provider credential manually."));
            } finally {
                archiveCredential(credential);
            }
        } else {
            throw new BadRequestException(String.format("Credential '%d' is in use, cannot be deleted.", credential.getId()));
        }
    }

    private Notification getCredentialNotification(Credential credential, String eventType, String message) {
        Notification notification = new Notification();
        notification.setEventType(eventType);
        notification.setEventTimestamp(Calendar.getInstance().getTime());
        notification.setEventMessage(message);
        notification.setOwner(credential.getOwner());
        notification.setAccount(credential.getAccount());
        return notification;

    }

    private String generateArchiveName(String name) {
        //generate new name for the archived credential to by pass unique constraint
        return new StringBuilder().append(name).append("_").append(UUID.randomUUID()).toString();
    }

    private void archiveCredential(Credential credential) {
        credential.setName(generateArchiveName(credential.getName()));
        credential.setArchived(true);
        credentialRepository.save(credential);
    }
}
