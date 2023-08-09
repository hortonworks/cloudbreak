package com.sequenceiq.cloudbreak.rotation.service.phase;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.google.common.collect.Lists;
import com.sequenceiq.cloudbreak.rotation.SecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.common.RotationContext;
import com.sequenceiq.cloudbreak.rotation.service.RotationMetadata;

@Service
public class SecretRotationFinalizeService extends AbstractSecretRotationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SecretRotationFinalizeService.class);

    public void finalize(RotationMetadata metadata) {
        Map<SecretRotationStep, ? extends RotationContext> contexts = getContexts(metadata.secretType(), metadata.resourceCrn());
        LOGGER.info("Contexts generation for secret rotation's post validation and finalization of {} regarding resource {} is finished.",
                metadata.secretType(), metadata.resourceCrn());
        Lists.reverse(metadata.secretType().getSteps()).forEach(step -> {
            LOGGER.info("Post validating rotation step {} for secret {} regarding resource {}.", step, metadata.secretType(), metadata.resourceCrn());
            getExecutor(step).executePostValidation(contexts.get(step));
        });
        Lists.reverse(metadata.secretType().getSteps()).forEach(step -> {
            LOGGER.info("Finalizing rotation step {} for secret {} regarding resource {}.", step, metadata.secretType(), metadata.resourceCrn());
            getExecutor(step).executeFinalize(contexts.get(step), metadata);
        });
    }
}
