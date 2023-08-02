package com.sequenceiq.cloudbreak.rotation.service.phase;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.rotation.SecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.common.RotationContext;
import com.sequenceiq.cloudbreak.rotation.service.RotationMetadata;

@Service
public class SecretRotationPreValidateService extends AbstractSecretRotationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SecretRotationPreValidateService.class);

    public void preValidate(RotationMetadata metadata) {
        Map<SecretRotationStep, ? extends RotationContext> contexts = getContexts(metadata.secretType(), metadata.resourceCrn());
        LOGGER.info("Contexts generation for validation before secret rotation of {} regarding resource {} is finished.",
                metadata.secretType(), metadata.resourceCrn());
        metadata.secretType().getSteps().forEach(step -> {
            LOGGER.info("Executing pre validation step {} for secret {} regarding resource {}", step, metadata.secretType(), metadata.resourceCrn());
            getExecutor(step).executePreValidation(contexts.get(step), metadata.secretType());
        });
    }
}
