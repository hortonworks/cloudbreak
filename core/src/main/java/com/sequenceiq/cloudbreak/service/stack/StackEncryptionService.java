package com.sequenceiq.cloudbreak.service.stack;

import java.util.Optional;

import jakarta.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.domain.stack.StackEncryption;
import com.sequenceiq.cloudbreak.repository.StackEncryptionRepository;

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
                new NotFoundException("Stack Encryption does not exist"));
    }

    public Optional<StackEncryption> findByStackId(Long stackId) {
        return stackEncryptionRepository.findStackEncryptionByStackId(stackId);
    }
}

