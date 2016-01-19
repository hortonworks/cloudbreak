package com.sequenceiq.cloudbreak.service.credential;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.inject.Inject;
import javax.transaction.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.type.APIResourceType;
import com.sequenceiq.cloudbreak.common.type.CbUserRole;
import com.sequenceiq.cloudbreak.controller.BadRequestException;
import com.sequenceiq.cloudbreak.controller.NotFoundException;
import com.sequenceiq.cloudbreak.domain.CbUser;
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.repository.CredentialRepository;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.service.DuplicateKeyValueException;
import com.sequenceiq.cloudbreak.service.notification.NotificationSender;
import com.sequenceiq.cloudbreak.service.stack.connector.adapter.ServiceProviderCredentialAdapter;

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
    private NotificationSender notificationSender;

    public Set<Credential> retrievePrivateCredentials(CbUser user) {
        return credentialRepository.findForUser(user.getUserId());
    }

    public Set<Credential> retrieveAccountCredentials(CbUser user) {
        if (user.getRoles().contains(CbUserRole.ADMIN)) {
            return credentialRepository.findAllInAccount(user.getAccount());
        } else {
            return credentialRepository.findPublicInAccountForUser(user.getUserId(), user.getAccount());
        }
    }

    @PostAuthorize("hasPermission(returnObject,'read')")
    public Credential get(Long id) {
        Credential credential = credentialRepository.findOne(id);
        if (credential == null) {
            throw new NotFoundException(String.format("Credential '%s' not found.", id));
        } else {
            return credential;
        }
    }

    @Transactional(Transactional.TxType.NEVER)
    public Credential create(CbUser user, Credential credential) {
        LOGGER.debug("Creating credential: [User: '{}', Account: '{}']", user.getUsername(), user.getAccount());
        credential.setOwner(user.getUserId());
        credential.setAccount(user.getAccount());
        credentialAdapter.init(credential);
        Credential savedCredential;
        try {
            savedCredential = credentialRepository.save(credential);
        } catch (DataIntegrityViolationException ex) {
            throw new DuplicateKeyValueException(APIResourceType.CREDENTIAL, credential.getName(), ex);
        }
        return savedCredential;
    }

    public Credential getPublicCredential(String name, CbUser user) {
        Credential credential = credentialRepository.findOneByName(name, user.getAccount());
        if (credential == null) {
            throw new NotFoundException(String.format("Credential '%s' not found.", name));
        } else {
            return credential;
        }
    }

    public Credential getPrivateCredential(String name, CbUser user) {
        Credential credential = credentialRepository.findByNameInUser(name, user.getUserId());
        if (credential == null) {
            throw new NotFoundException(String.format("Credential '%s' not found.", name));
        } else {
            return credential;
        }
    }

    @Transactional(Transactional.TxType.NEVER)
    public void delete(Long id, CbUser user) {
        Credential credential = credentialRepository.findByIdInAccount(id, user.getAccount());
        if (credential == null) {
            throw new NotFoundException(String.format("Credential '%s' not found.", id));
        }
        delete(credential, user);
    }

    @Transactional(Transactional.TxType.NEVER)
    public void delete(String name, CbUser user) {
        Credential credential = credentialRepository.findByNameInAccount(name, user.getAccount(), user.getUserId());
        if (credential == null) {
            throw new NotFoundException(String.format("Credential '%s' not found.", name));
        }
        delete(credential, user);
    }

    @Transactional(Transactional.TxType.NEVER)
    public Credential update(Long id) throws Exception {
        Credential credential = get(id);
        if (credential == null) {
            throw new NotFoundException(String.format("Credential '%s' not found.", id));
        } else {
            return credentialAdapter.update(credential);
        }
    }

    private void delete(Credential credential, CbUser user) {
        if (!user.getUserId().equals(credential.getOwner()) && !user.getRoles().contains(CbUserRole.ADMIN)) {
            throw new BadRequestException("Credentials can be deleted only by account admins or owners.");
        }
        List<Stack> stacks = stackRepository.findByCredential(credential.getId());
        if (stacks.isEmpty()) {
            archiveCredential(credential);
        } else {
            throw new BadRequestException(String.format("Credential '%d' is in use, cannot be deleted.", credential.getId()));
        }
    }

    private String generateArchiveName(String name) {
        //generate new name for the archived credential to by pass unique constraint
        return new StringBuilder().append(name).append("_").append(UUID.randomUUID()).toString();
    }

    private void archiveCredential(Credential credential) {
        credential.setName(generateArchiveName(credential.getName()));
        credential.setArchived(true);
        credential.setTopology(null);
        credentialRepository.save(credential);
    }
}
