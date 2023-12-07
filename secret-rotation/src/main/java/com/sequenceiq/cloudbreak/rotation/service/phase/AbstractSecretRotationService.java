package com.sequenceiq.cloudbreak.rotation.service.phase;

import static com.sequenceiq.cloudbreak.rotation.entity.SecretRotationStepProgressStatus.FAILED;
import static com.sequenceiq.cloudbreak.rotation.entity.SecretRotationStepProgressStatus.FINISHED;
import static com.sequenceiq.cloudbreak.rotation.entity.SecretRotationStepProgressStatus.IN_PROGRESS;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import com.sequenceiq.cloudbreak.rotation.SecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.SecretType;
import com.sequenceiq.cloudbreak.rotation.common.RotationContext;
import com.sequenceiq.cloudbreak.rotation.common.RotationContextProvider;
import com.sequenceiq.cloudbreak.rotation.entity.SecretRotationStepProgress;
import com.sequenceiq.cloudbreak.rotation.executor.AbstractRotationExecutor;
import com.sequenceiq.cloudbreak.rotation.service.RotationMetadata;
import com.sequenceiq.cloudbreak.rotation.service.progress.SecretRotationStepProgressService;

abstract class AbstractSecretRotationService {

    @Inject
    private Map<SecretRotationStep, AbstractRotationExecutor<? extends RotationContext>> rotationExecutorMap;

    @Inject
    private Map<SecretType, RotationContextProvider> rotationContextProviderMap;

    @Inject
    private SecretRotationStepProgressService stepProgressService;

    protected abstract List<SecretRotationStep> getStepsByProgress(RotationMetadata metadata);

    protected void checkIfExecutionValidByProgress(RotationMetadata metadata) {
        if (!stepProgressService.executionValidByProgress(metadata)) {
            throw new RuntimeException("The current execution phase of the rotation is not correct based on the latest information in database");
        }
    }

    protected Map<SecretRotationStep, ? extends RotationContext> getContexts(SecretType secretType, String resourceCrn,
            Map<String, String> additionalProperties) {
        Map<SecretRotationStep, ? extends RotationContext> contexts = rotationContextProviderMap.get(secretType).
                getContextsWithProperties(resourceCrn, additionalProperties);
        if (!contexts.keySet().containsAll(secretType.getSteps())) {
            throw new RuntimeException("At least one context is missing thus secret rotation flow step cannot be executed.");
        }
        return contexts;
    }

    protected AbstractRotationExecutor getExecutor(SecretRotationStep step) {
        return rotationExecutorMap.get(step);
    }

    protected RotationContextProvider getContextProvider(RotationMetadata metadata) {
        return rotationContextProviderMap.get(metadata.secretType());
    }

    protected SecretRotationStepProgressService getProgressService() {
        return stepProgressService;
    }

    protected abstract void executePhase(RotationMetadata metadata, SecretRotationStep step, RotationContext context);

    protected void executePhaseWithProgressCheck(RotationMetadata metadata, SecretRotationStep step, RotationContext context) {
        getProgressService().update(metadata, step, IN_PROGRESS);
        try {
            executePhase(metadata, step, context);
            getProgressService().update(metadata, step, FINISHED);
        } catch (Exception e) {
            getProgressService().update(metadata, step, FAILED);
            throw e;
        }
    }

    protected static List<SecretRotationStep> getStepsByCurrentRotationStatus(List<SecretRotationStep> steps, SecretRotationStepProgress progress) {
        final int indexOfStepFromProgress = steps.indexOf(progress.getSecretRotationStep());
        return switch (progress.getStatus()) {
            case IN_PROGRESS, FAILED -> steps.subList(indexOfStepFromProgress, steps.size());
            case FINISHED -> indexOfStepFromProgress == steps.size() - 1 ? List.of() : steps.subList(indexOfStepFromProgress + 1, steps.size());
        };
    }
}
