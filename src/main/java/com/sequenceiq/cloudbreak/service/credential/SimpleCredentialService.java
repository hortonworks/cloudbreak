package com.sequenceiq.cloudbreak.service.credential;

import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.controller.BadRequestException;
import com.sequenceiq.cloudbreak.controller.NotFoundException;
import com.sequenceiq.cloudbreak.domain.APIResourceType;
import com.sequenceiq.cloudbreak.domain.CbUser;
import com.sequenceiq.cloudbreak.domain.CbUserRole;
import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.repository.CredentialRepository;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.service.DuplicateKeyValueException;

@Service
public class SimpleCredentialService implements CredentialService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleCredentialService.class);

    @Autowired
    private CredentialRepository credentialRepository;

    @Autowired
    private StackRepository stackRepository;

    @Resource
    private Map<CloudPlatform, CredentialHandler<Credential>> credentialHandlers;

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
        MDCBuilder.buildMdcContext(credential);
        LOGGER.debug("Creating credential: [User: '{}', Account: '{}']", user.getUsername(), user.getAccount());
        credentialHandlers.get(credential.cloudPlatform()).init(credential);
        credential.setOwner(user.getUserId());
        credential.setAccount(user.getAccount());
        Credential savedCredential;
        try {
            savedCredential = credentialRepository.save(credential);
        } catch (DataIntegrityViolationException ex) {
            throw new DuplicateKeyValueException(APIResourceType.CREDENTIAL, credential.getName(), ex);
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
        Credential credential = credentialRepository.findOne(id);
        if (credential == null) {
            throw new NotFoundException(String.format("Credential '%s' not found.", id));
        } else {
            return credentialHandlers.get(credential.cloudPlatform()).update(credential);
        }
    }

    private void delete(Credential credential, CbUser user) {
        if (!user.getUserId().equals(credential.getOwner()) && !user.getRoles().contains(CbUserRole.ADMIN)) {
            throw new BadRequestException("Credentials can be deleted only by account admins or owners.");
        }
        List<Stack> stacks = stackRepository.findByCredential(credential.getId());
        if (stacks.isEmpty()) {
            credentialHandlers.get(credential.cloudPlatform()).delete(credential);
            credentialRepository.delete(credential);
        } else {
            if (stacks.isEmpty()) {
                credentialHandlers.get(credential.cloudPlatform()).delete(credential);
                credentialRepository.delete(credential);
            } else {
                throw new BadRequestException(String.format("Credential '%d' is in use, cannot be deleted.", credential.getId()));
            }
        }
    }
}
