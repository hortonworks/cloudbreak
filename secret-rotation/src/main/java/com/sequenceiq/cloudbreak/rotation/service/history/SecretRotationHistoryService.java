package com.sequenceiq.cloudbreak.rotation.service.history;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import jakarta.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.rotation.SecretType;
import com.sequenceiq.cloudbreak.rotation.entity.SecretRotationHistory;
import com.sequenceiq.cloudbreak.rotation.repository.SecretRotationHistoryRepository;
import com.sequenceiq.cloudbreak.rotation.service.RotationMetadata;

@Service
public class SecretRotationHistoryService {

    private static final int ROTATION_DUE_BUFFER_DAYS = 10;

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

    public boolean checkIfRotationDue(String resourceCrn, SecretType secretType, Duration secretInterval, Instant resourceCreationDate) {
        Optional<SecretRotationHistory> secretRotationHistory =
            repository.findByResourceCrnAndSecretType(resourceCrn, secretType);
        if (secretInterval == null || secretInterval.isZero() || secretInterval.isNegative()) {
            return false;
        }
        Instant now = Instant.now();
        // Add buffer so rotations can start ahead of time in case clusters are unavailable/stopped.
        // Mark as due if the next rotation time is within the next ROTATION_DUE_BUFFER_DAYS days.
        Instant threshold = now.plus(Duration.ofDays(ROTATION_DUE_BUFFER_DAYS));
        // When no history, treat creation date as the date when the secret was last rotated
        Instant lastUpdated = secretRotationHistory
                .map(h -> Instant.ofEpochMilli(h.getLastUpdated()))
                .orElse(resourceCreationDate);
        Instant nextDue = lastUpdated.plus(secretInterval);
        return !nextDue.isAfter(threshold);
    }
}
