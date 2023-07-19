package com.sequenceiq.cloudbreak.rotation.service.phase;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.sequenceiq.cloudbreak.rotation.SecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.common.RotationContext;
import com.sequenceiq.cloudbreak.rotation.service.RotationMetadata;
import com.sequenceiq.cloudbreak.vault.ThreadBasedVaultReadFieldProvider;

@Service
public class SecretRotationRollbackService extends AbstractSecretRotationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SecretRotationRollbackService.class);

    public void rollback(RotationMetadata metadata, SecretRotationStep failedStep) {
        Map<SecretRotationStep, ? extends RotationContext> contexts = getContexts(metadata.secretType(), metadata.resourceCrn());
        LOGGER.info("Contexts generation for secret rotation's rollback of {} regarding resource {} is finished.",
                metadata.secretType(), metadata.resourceCrn());
        Set<String> affectedSecrets = getContextProvider(metadata).getVaultSecretsForRollback(metadata.resourceCrn(), contexts);
        List<SecretRotationStep> rollbackSteps = getRollbackSteps(metadata, failedStep);

        rollbackSteps.forEach(step -> {
            RotationContext rotationContext = contexts.get(step);
            LOGGER.info("Rolling back rotation step {} for secret {} regarding resource {}.", step, metadata.secretType(), metadata.resourceCrn());
            LOGGER.trace("Affected secrets of rotation's rollback: {}", Joiner.on(",").join(affectedSecrets));
            ThreadBasedVaultReadFieldProvider.doRollback(affectedSecrets, () ->
                    getExecutor(step).executeRollback(rotationContext, metadata.secretType()));
        });
    }

    private List<SecretRotationStep> getRollbackSteps(RotationMetadata rotationMetadata, SecretRotationStep failedStep) {
        List<SecretRotationStep> steps = rotationMetadata.secretType().getSteps();
        List<SecretRotationStep> reversedSteps;
        if (failedStep != null) {
            LOGGER.info("Rollback of secret rotation is starting from step {}, since rotation is failed there (secret is {}, resource is {})",
                    failedStep, rotationMetadata.secretType(), rotationMetadata.resourceCrn());
            reversedSteps = Lists.reverse(steps.subList(0, steps.indexOf(failedStep) + 1));
        } else {
            reversedSteps = Lists.reverse(steps);
        }
        return reversedSteps;
    }
}
