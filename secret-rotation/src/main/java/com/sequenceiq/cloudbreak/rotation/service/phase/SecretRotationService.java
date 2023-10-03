package com.sequenceiq.cloudbreak.rotation.service.phase;

import static com.sequenceiq.cloudbreak.rotation.RotationFlowExecutionType.PREVALIDATE;
import static com.sequenceiq.cloudbreak.rotation.RotationFlowExecutionType.ROTATE;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.rotation.SecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.common.RotationContext;
import com.sequenceiq.cloudbreak.rotation.entity.SecretRotationStepProgress;
import com.sequenceiq.cloudbreak.rotation.service.RotationMetadata;

@Service
public class SecretRotationService extends AbstractSecretRotationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SecretRotationService.class);

    public void rotate(RotationMetadata metadata) {
        checkIfExecutionValidByProgress(metadata);
        Map<SecretRotationStep, ? extends RotationContext> contexts = getContexts(metadata.secretType(), metadata.resourceCrn());
        LOGGER.info("Contexts generation for secret rotation of {} regarding resource {} is finished.", metadata.secretType(), metadata.resourceCrn());
        getStepsByProgress(metadata).forEach(step -> executePhaseWithProgressCheck(metadata, step, contexts.get(step)));
    }

    @Override
    protected void executePhase(RotationMetadata metadata, SecretRotationStep step, RotationContext context) {
        LOGGER.info("Executing rotation step {} for secret {} regarding resource {}", step, metadata.secretType(), metadata.resourceCrn());
        getExecutor(step).executeRotate(context, metadata);
    }

    @Override
    protected List<SecretRotationStep> getStepsByProgress(RotationMetadata metadata) {
        List<SecretRotationStep> steps = metadata.secretType().getSteps();
        Optional<SecretRotationStepProgress> progress = getProgressService().getProgress(metadata);
        if (progress.isEmpty() || PREVALIDATE.equals(progress.get().getCurrentExecutionType())) {
            return steps;
        } else if (ROTATE.equals(progress.get().getCurrentExecutionType())) {
            return getStepsByCurrentRotationStatus(steps, progress.get());
        }
        return steps;
    }
}
