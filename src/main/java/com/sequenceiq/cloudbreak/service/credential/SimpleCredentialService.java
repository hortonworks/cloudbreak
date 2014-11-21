package com.sequenceiq.cloudbreak.service.credential;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.controller.NotFoundException;
import com.sequenceiq.cloudbreak.domain.CbUser;
import com.sequenceiq.cloudbreak.domain.CbUserRole;
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.repository.CredentialRepository;
import com.sequenceiq.cloudbreak.service.DuplicateKeyValueException;

@Service
public class SimpleCredentialService implements CredentialService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleCredentialService.class);

    @Autowired
    private CredentialRepository credentialRepository;

    @Override
    public Set<Credential> retrievePrivateCredentials(CbUser user) {
        return credentialRepository.findForUser(user.getUserId());
    }

    @Override
    public Set<Credential> retrieveAccountCredentials(CbUser user) {
        if (user.getRoles().contains(CbUserRole.ADMIN)) {
            return credentialRepository.findAllInAccount(user.getAccount());
        } else {
            return credentialRepository.findPublicsInAccount(user.getAccount());
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
        Credential savedCredential = null;
        credential.setOwner(user.getUserId());
        credential.setAccount(user.getAccount());
        try {
            savedCredential = credentialRepository.save(credential);
        } catch (DataIntegrityViolationException ex) {
            throw new DuplicateKeyValueException(credential.getName(), ex);
        }

        return savedCredential;
    }

    @Override
    public void delete(Long id) {
        Credential credential = credentialRepository.findOne(id);
        if (credential == null) {
            throw new NotFoundException(String.format("Credential '%s' not found.", id));
        }
        credentialRepository.delete(credential);
    }
}
