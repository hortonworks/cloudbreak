package com.sequenceiq.cloudbreak.rotation.service.progress;

import java.util.List;
import java.util.Optional;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.rotation.SecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.SecretType;
import com.sequenceiq.cloudbreak.rotation.entity.SecretRotationStepProgress;
import com.sequenceiq.cloudbreak.rotation.repository.SecretRotationStepProgressRepository;
import com.sequenceiq.cloudbreak.rotation.service.RotationMetadata;

@Service
public class SecretRotationStepProgressService {

    @Inject
    private SecretRotationStepProgressRepository repository;

    public List<SecretRotationStepProgress> listStepsProgressByRotation(RotationMetadata metadata) {
        return repository.findByResourceCrnAndSecretType(metadata.resourceCrn(), metadata.secretType());
    }

    public void finished(SecretRotationStepProgress entity) {
        entity.setFinished(System.currentTimeMillis());
        repository.save(entity);
    }

    public List<SecretRotationStepProgress> listSteps(String resourceCrn, SecretType secretType) {
        return repository.findByResourceCrnAndSecretType(resourceCrn, secretType);
    }

    public Optional<SecretRotationStepProgress> latestStep(RotationMetadata metadata, SecretRotationStep step) {
        Optional<SecretRotationStepProgress> latestStepProgress = repository.findByResourceCrnAndExecutionTypeAndSecretTypeAndSecretRotationStep(
                metadata.resourceCrn(), metadata.currentExecution(), metadata.secretType(), step);
        if (latestStepProgress.isEmpty()) {
            SecretRotationStepProgress progress =
                    new SecretRotationStepProgress(metadata.resourceCrn(), metadata.secretType(), step, metadata.currentExecution(), System.currentTimeMillis());
            return Optional.of(repository.save(progress));
        }
        return latestStepProgress;
    }

    public void deleteAllForCurrentRotation(String resourceCrn, SecretType secretType) {
        repository.deleteByResourceCrnAndSecretType(resourceCrn, secretType);
    }
}
