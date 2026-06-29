package com.sequenceiq.cloudbreak.rotation.context.provider;

import static com.sequenceiq.cloudbreak.rotation.CommonSecretRotationStep.CUSTOM_JOB;

import java.util.Map;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.rotation.RotationNodeValidationService;
import com.sequenceiq.cloudbreak.rotation.SecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.common.RotationContext;
import com.sequenceiq.cloudbreak.rotation.common.RotationContextProvider;
import com.sequenceiq.cloudbreak.rotation.secret.custom.CustomJobRotationContext;

public abstract class AbstractCMHostCertRotationContextProvider implements RotationContextProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractCMHostCertRotationContextProvider.class);

    @Inject
    private RotationNodeValidationService rotationNodeValidationService;

    @Override
    public Map<SecretRotationStep, RotationContext> getContexts(String resourceCrn) {
        return Map.of(CUSTOM_JOB, getCustomJobRotationContext(resourceCrn));
    }

    private CustomJobRotationContext getCustomJobRotationContext(String resourceCrn) {
        return CustomJobRotationContext.builder()
                .withResourceCrn(resourceCrn)
                .withPreValidateJob(() -> rotationNodeValidationService.validateNoStoppedInstances(resourceCrn, getSecret()))
                .withRotationJob(() -> LOGGER.info("{} will be executed with different flow.", getRotationTypeMessage()))
                .build();
    }

    protected abstract String getRotationTypeMessage();
}
