package com.sequenceiq.cloudbreak.rotation.service.phase;

import static com.sequenceiq.cloudbreak.rotation.RotationFlowExecutionType.ROLLBACK;
import static com.sequenceiq.cloudbreak.rotation.RotationFlowExecutionType.ROTATE;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.google.common.collect.Lists;
import com.sequenceiq.cloudbreak.rotation.SecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.common.RotationContext;
import com.sequenceiq.cloudbreak.rotation.entity.SecretRotationStepProgress;
import com.sequenceiq.cloudbreak.rotation.service.RotationMetadata;
import com.sequenceiq.cloudbreak.vault.ThreadBasedVaultReadFieldProvider;

@Service
public class SecretRotationRollbackService extends AbstractSecretRotationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SecretRotationRollbackService.class);

    public void rollback(RotationMetadata metadata) {
        checkIfExecutionValidByProgress(metadata);
        Map<SecretRotationStep, ? extends RotationContext> contexts = getContexts(metadata.secretType(), metadata.resourceCrn(),
                metadata.additionalProperties());
        LOGGER.info("Contexts generation for secret rotation's rollback of {} regarding resource {} is finished.",
                metadata.secretType(), metadata.resourceCrn());
        Set<String> affectedSecrets = getContextProvider(metadata).getVaultSecretsForRollback(metadata.resourceCrn(), contexts);
        getStepsByProgress(metadata).forEach(step ->
                ThreadBasedVaultReadFieldProvider.doRollback(affectedSecrets, () -> executePhaseWithProgressCheck(metadata, step, contexts.get(step))));
    }

    @Override
    protected void executePhase(RotationMetadata metadata, SecretRotationStep step, RotationContext context) {
        LOGGER.info("Rolling back rotation step {} for secret {} regarding resource {}.", step, metadata.secretType(), metadata.resourceCrn());
        getExecutor(step).executeRollback(context, metadata);
    }

    @Override
    protected List<SecretRotationStep> getStepsByProgress(RotationMetadata metadata) {
        List<SecretRotationStep> steps = Lists.reverse(metadata.secretType().getSteps());
        Optional<SecretRotationStepProgress> progress = getProgressService().getProgress(metadata);
        if (progress.isEmpty()) {
            return steps;
        } else if (ROTATE.equals(progress.get().getCurrentExecutionType())) {
            return steps.subList(steps.indexOf(progress.get().getSecretRotationStep()), steps.size());
        } else if (ROLLBACK.equals(progress.get().getCurrentExecutionType())) {
            return getStepsByCurrentRotationStatus(steps, progress.get());
        }
        return steps;
    }
}
