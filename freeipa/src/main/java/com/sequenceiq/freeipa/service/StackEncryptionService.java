package com.sequenceiq.freeipa.service;

import jakarta.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.freeipa.entity.StackEncryption;
import com.sequenceiq.freeipa.repository.StackEncryptionRepository;

@Service
public class StackEncryptionService {

    @Inject
    private StackEncryptionRepository stackEncryptionRepository;

    public StackEncryption save(StackEncryption stackEncryption) {
        return stackEncryptionRepository.save(stackEncryption);
    }

    public void deleteStackEncryption(Long stackId) {
        stackEncryptionRepository.deleteStackEncryptionByStackId(stackId);
    }

    public StackEncryption getStackEncryption(Long stackId) {
        return stackEncryptionRepository.findStackEncryptionByStackId(stackId).orElseThrow(() ->
                new CloudbreakServiceException("Stack Encryption does not exist"));
    }
}
