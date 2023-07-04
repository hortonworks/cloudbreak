package com.sequenceiq.cloudbreak.rotation.service;

import java.util.Optional;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.rotation.RotationFlowExecutionType;
import com.sequenceiq.cloudbreak.rotation.SecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.SecretType;
import com.sequenceiq.cloudbreak.rotation.entity.SecretRotationStepProgress;
import com.sequenceiq.cloudbreak.rotation.repository.SecretRotationStepProgressRepository;

@Service
public class SecretRotationStepProgressService implements SecretRotationProgressService<SecretRotationStepProgress> {

    @Inject
    private Optional<SecretRotationStepProgressRepository> repository;

    @Override
    public boolean isFinished(SecretRotationStepProgress entity) {
        return entity.getFinished() != null;
    }

    @Override
    public void finished(SecretRotationStepProgress entity) {
        repository.ifPresent(repo -> {
            entity.setFinished(System.currentTimeMillis());
            repo.save(entity);
        });
    }

    @Override
    public Optional<SecretRotationStepProgress> latestStep(String resourceCrn, SecretType secretType,
            SecretRotationStep step, RotationFlowExecutionType executionType) {
        if (repository.isPresent()) {
            Optional<SecretRotationStepProgress> latestStepProgress = repository.get().findAllByResourceCrnAndExecutionType(resourceCrn, executionType)
                    .stream()
                    .filter(progress -> secretType.equals(progress.getSecretType()) && step.equals(progress.getSecretRotationStep()))
                    .findFirst();
            if (latestStepProgress.isEmpty()) {
                SecretRotationStepProgress progress = new SecretRotationStepProgress(resourceCrn, secretType, step, executionType, System.currentTimeMillis());
                return Optional.of(repository.get().save(progress));
            }
            return latestStepProgress;
        }
        return Optional.empty();
    }

    @Override
    public void deleteAll(String resourceCrn, SecretType secretType) {
        repository.ifPresent(repo -> repo.deleteByResourceCrnAndSecretType(resourceCrn, secretType));
    }
}
