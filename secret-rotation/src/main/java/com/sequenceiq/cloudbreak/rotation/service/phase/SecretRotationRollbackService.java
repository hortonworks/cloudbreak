package com.sequenceiq.cloudbreak.rotation.service.phase;

import static com.sequenceiq.cloudbreak.rotation.RotationFlowExecutionType.ROTATE;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.google.common.base.Joiner;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.sequenceiq.cloudbreak.rotation.SecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.common.RotationContext;
import com.sequenceiq.cloudbreak.rotation.entity.SecretRotationStepProgress;
import com.sequenceiq.cloudbreak.rotation.service.RotationMetadata;
import com.sequenceiq.cloudbreak.rotation.service.progress.SecretRotationStepProgressService;
import com.sequenceiq.cloudbreak.vault.ThreadBasedVaultReadFieldProvider;

@Service
public class SecretRotationRollbackService extends AbstractSecretRotationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SecretRotationRollbackService.class);

    @Inject
    private SecretRotationStepProgressService stepProgressService;

    public void rollback(RotationMetadata metadata) {
        Map<SecretRotationStep, ? extends RotationContext> contexts = getContexts(metadata.secretType(), metadata.resourceCrn());
        LOGGER.info("Contexts generation for secret rotation's rollback of {} regarding resource {} is finished.",
                metadata.secretType(), metadata.resourceCrn());
        Set<String> affectedSecrets = getContextProvider(metadata).getVaultSecretsForRollback(metadata.resourceCrn(), contexts);
        List<SecretRotationStep> rollbackSteps = getRollbackSteps(metadata);

        rollbackSteps.forEach(step -> {
            RotationContext rotationContext = contexts.get(step);
            LOGGER.info("Rolling back rotation step {} for secret {} regarding resource {}.", step, metadata.secretType(), metadata.resourceCrn());
            LOGGER.trace("Affected secrets of rotation's rollback: {}", Joiner.on(",").join(affectedSecrets));
            ThreadBasedVaultReadFieldProvider.doRollback(affectedSecrets, () ->
                    getExecutor(step).executeRollback(rotationContext, metadata));
        });
    }

    private List<SecretRotationStep> getRollbackSteps(RotationMetadata rotationMetadata) {
        List<SecretRotationStep> steps = rotationMetadata.secretType().getSteps();
        SecretRotationStep failedStep = stepProgressService.listStepsProgressByRotation(rotationMetadata)
                .stream()
                .filter(step -> ROTATE.equals(step.getExecutionType()))
                .max(Comparator.comparing(SecretRotationStepProgress::getFinished))
                .map(SecretRotationStepProgress::getSecretRotationStep)
                .orElse(Iterables.getLast(rotationMetadata.secretType().getSteps()));
        LOGGER.info("Rollback of secret rotation is starting from step {}, since rotation is failed there (secret is {}, resource is {})",
                failedStep, rotationMetadata.secretType(), rotationMetadata.resourceCrn());
        return Lists.reverse(steps.subList(0, steps.indexOf(failedStep) + 1));
    }
}
