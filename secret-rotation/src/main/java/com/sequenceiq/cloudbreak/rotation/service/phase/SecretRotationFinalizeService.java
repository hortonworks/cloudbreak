package com.sequenceiq.cloudbreak.rotation.service.phase;

import static com.sequenceiq.cloudbreak.rotation.RotationFlowExecutionType.FINALIZE;
import static com.sequenceiq.cloudbreak.rotation.RotationFlowExecutionType.ROTATE;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.google.common.collect.Lists;
import com.sequenceiq.cloudbreak.rotation.SecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.common.RotationContext;
import com.sequenceiq.cloudbreak.rotation.entity.SecretRotationStepProgress;
import com.sequenceiq.cloudbreak.rotation.executor.AbstractRotationExecutor;
import com.sequenceiq.cloudbreak.rotation.service.RotationMetadata;

@Service
public class SecretRotationFinalizeService extends AbstractSecretRotationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SecretRotationFinalizeService.class);

    public void finalize(RotationMetadata metadata) {
        checkIfExecutionValidByProgress(metadata);
        Map<SecretRotationStep, ? extends RotationContext> contexts = getContexts(metadata.secretType(), metadata.resourceCrn(),
                metadata.additionalProperties());
        LOGGER.info("Contexts generation for secret rotation's post validation and finalization of {} regarding resource {} is finished.",
                metadata.secretType(), metadata.resourceCrn());
        getStepsByProgress(metadata).forEach(step -> executePhaseWithProgressCheck(metadata, step, contexts.get(step)));
    }

    @Override
    protected void executePhase(RotationMetadata metadata, SecretRotationStep step, RotationContext context) {
        AbstractRotationExecutor rotationExecutor = getExecutor(step);
        LOGGER.info("Post validating rotation step {} for secret {} regarding resource {}.", step, metadata.secretType(), metadata.resourceCrn());
        rotationExecutor.executePostValidation(context, metadata);
        LOGGER.info("Finalizing rotation step {} for secret {} regarding resource {}.", step, metadata.secretType(), metadata.resourceCrn());
        rotationExecutor.executeFinalize(context, metadata);
    }

    @Override
    protected List<SecretRotationStep> getStepsByProgress(RotationMetadata metadata) {
        List<SecretRotationStep> steps = Lists.reverse(metadata.secretType().getSteps());
        Optional<SecretRotationStepProgress> progress = getProgressService().getProgress(metadata);
        if (progress.isEmpty() || ROTATE.equals(progress.get().getCurrentExecutionType())) {
            return steps;
        } else if (FINALIZE.equals(progress.get().getCurrentExecutionType())) {
            return getStepsByCurrentRotationStatus(steps, progress.get());
        }
        return steps;
    }
}
