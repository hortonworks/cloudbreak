package com.sequenceiq.cloudbreak.rotation.service.progress;

import static com.sequenceiq.cloudbreak.rotation.RotationFlowExecutionType.FINALIZE;
import static com.sequenceiq.cloudbreak.rotation.RotationFlowExecutionType.PREVALIDATE;
import static com.sequenceiq.cloudbreak.rotation.RotationFlowExecutionType.ROLLBACK;
import static com.sequenceiq.cloudbreak.rotation.RotationFlowExecutionType.ROTATE;
import static com.sequenceiq.cloudbreak.rotation.entity.SecretRotationStepProgressStatus.FAILED;
import static com.sequenceiq.cloudbreak.rotation.entity.SecretRotationStepProgressStatus.FINISHED;

import java.util.Optional;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.google.common.collect.Iterables;
import com.sequenceiq.cloudbreak.rotation.RotationFlowExecutionType;
import com.sequenceiq.cloudbreak.rotation.SecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.SecretType;
import com.sequenceiq.cloudbreak.rotation.entity.SecretRotationStepProgress;
import com.sequenceiq.cloudbreak.rotation.entity.SecretRotationStepProgressStatus;
import com.sequenceiq.cloudbreak.rotation.repository.SecretRotationStepProgressRepository;
import com.sequenceiq.cloudbreak.rotation.service.RotationMetadata;

@Service
public class SecretRotationStepProgressService {

    @Inject
    private SecretRotationStepProgressRepository repository;

    public Optional<SecretRotationStepProgress> getProgress(String resourceCrn, SecretType secretType) {
        return repository.findByResourceCrnAndSecretType(resourceCrn, secretType);
    }

    public Optional<SecretRotationStepProgress> getProgress(RotationMetadata metadata) {
        return getProgress(metadata.resourceCrn(), metadata.secretType());
    }

    public SecretRotationStepProgress update(RotationMetadata metadata, SecretRotationStep step, SecretRotationStepProgressStatus status) {
        SecretRotationStepProgress progress = updateProgress(getProgress(metadata).orElse(new SecretRotationStepProgress()), metadata, step, status);
        return repository.save(progress);
    }

    public void deleteCurrentRotation(RotationMetadata metadata) {
        repository.deleteByResourceCrnAndSecretType(metadata.resourceCrn(), metadata.secretType());
    }

    public void deleteAllForResource(String resourceCrn) {
        repository.deleteAllByResourceCrn(resourceCrn);
    }

    public boolean executionValidByProgress(RotationMetadata metadata) {
        Optional<SecretRotationStepProgress> progress = getProgress(metadata);
        return switch (metadata.currentExecution()) {
            case PREVALIDATE -> executionValidForPreValidate(progress);
            case ROTATE -> executionValidForRotate(progress);
            case ROLLBACK -> executionValidForRollback(progress);
            case FINALIZE -> executionValidForFinalize(progress);
        };
    }

    private static boolean executionValidForFinalize(Optional<SecretRotationStepProgress> progress) {
        return progress.isPresent() &&
                (previousExecutionTypeFinished(ROTATE, progress.get()) || FINALIZE.equals(progress.get().getCurrentExecutionType()));
    }

    private static boolean executionValidForRollback(Optional<SecretRotationStepProgress> progress) {
        return progress.isPresent() &&
                (ROTATE.equals(progress.get().getCurrentExecutionType()) && FAILED.equals(progress.get().getStatus()) ||
                        ROLLBACK.equals(progress.get().getCurrentExecutionType()));
    }

    private static boolean executionValidForPreValidate(Optional<SecretRotationStepProgress> progress) {
        return progress.isEmpty() || PREVALIDATE.equals(progress.get().getCurrentExecutionType());
    }

    private static boolean executionValidForRotate(Optional<SecretRotationStepProgress> progress) {
        return progress.isPresent() &&
                (previousExecutionTypeFinished(PREVALIDATE, progress.get()) || ROTATE.equals(progress.get().getCurrentExecutionType()));
    }

    private static boolean previousExecutionTypeFinished(RotationFlowExecutionType expectedPreviousExecutionType, SecretRotationStepProgress progress) {
        return expectedPreviousExecutionType.equals(progress.getCurrentExecutionType()) && FINISHED.equals(progress.getStatus()) &&
                progress.getSecretRotationStep().equals(Iterables.getLast(progress.getSecretType().getSteps()));
    }

    private static SecretRotationStepProgress updateProgress(SecretRotationStepProgress progress, RotationMetadata metadata, SecretRotationStep step,
            SecretRotationStepProgressStatus status) {
        progress.setCurrentExecutionType(metadata.currentExecution());
        progress.setSecretRotationStep(step);
        progress.setStatus(status);
        progress.setSecretType(metadata.secretType());
        progress.setResourceCrn(metadata.resourceCrn());
        return progress;
    }
}
