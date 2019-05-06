package com.sequenceiq.freeipa.service;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.freeipa.api.model.credential.CredentialRequest;
import com.sequenceiq.freeipa.converter.credential.CredentialV4RequestToCredentialConverter;
import com.sequenceiq.freeipa.entity.Credential;
import com.sequenceiq.freeipa.repository.CredentialRepository;

@Service
public class CredentialService {

    @Inject
    private CredentialRepository credentialRepository;

    @Inject
    private CredentialV4RequestToCredentialConverter credentialConverter;

    public Credential create(CredentialRequest request) {
        Credential credential = credentialConverter.convert(request);
        return credentialRepository.save(credential);
    }
}
