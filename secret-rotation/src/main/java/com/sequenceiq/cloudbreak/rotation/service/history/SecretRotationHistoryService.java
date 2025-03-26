package com.sequenceiq.cloudbreak.rotation.service.history;

import java.util.List;
import java.util.Optional;

import jakarta.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.rotation.entity.SecretRotationHistory;
import com.sequenceiq.cloudbreak.rotation.repository.SecretRotationHistoryRepository;
import com.sequenceiq.cloudbreak.rotation.service.RotationMetadata;

@Service
public class SecretRotationHistoryService {

    @Inject
    private SecretRotationHistoryRepository repository;

    public void addHistoryItem(RotationMetadata metadata) {
        Optional<SecretRotationHistory> secretRotationHistory =
                repository.findByResourceCrnAndSecretType(metadata.resourceCrn(), metadata.secretType());
        if (secretRotationHistory.isPresent()) {
            SecretRotationHistory latest = secretRotationHistory.get();
            latest.setLastUpdated(System.currentTimeMillis());
            repository.save(latest);
        } else {
            repository.save(new SecretRotationHistory(metadata.resourceCrn(), metadata.secretType(), System.currentTimeMillis()));
        }
    }

    public List<SecretRotationHistory> getHistoryForResource(String resourceCrn) {
        return repository.findByResourceCrn(resourceCrn);
    }
}
