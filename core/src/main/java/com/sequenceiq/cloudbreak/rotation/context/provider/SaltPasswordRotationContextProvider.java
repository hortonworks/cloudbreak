package com.sequenceiq.cloudbreak.rotation.context.provider;

import static com.sequenceiq.cloudbreak.rotation.CommonSecretRotationStep.CUSTOM_JOB;

import java.util.Map;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorException;
import com.sequenceiq.cloudbreak.rotation.CloudbreakSecretType;
import com.sequenceiq.cloudbreak.rotation.SecretRotationSaltService;
import com.sequenceiq.cloudbreak.rotation.SecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.SecretType;
import com.sequenceiq.cloudbreak.rotation.common.RotationContext;
import com.sequenceiq.cloudbreak.rotation.common.RotationContextProvider;
import com.sequenceiq.cloudbreak.rotation.common.SecretRotationException;
import com.sequenceiq.cloudbreak.rotation.secret.custom.CustomJobRotationContext;
import com.sequenceiq.cloudbreak.service.salt.RotateSaltPasswordService;
import com.sequenceiq.cloudbreak.service.salt.RotateSaltPasswordValidator;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;

@Component
public class SaltPasswordRotationContextProvider implements RotationContextProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(SaltPasswordRotationContextProvider.class);

    @Inject
    private StackDtoService stackDtoService;

    @Inject
    private RotateSaltPasswordValidator rotateSaltPasswordValidator;

    @Inject
    private RotateSaltPasswordService rotateSaltPasswordService;

    @Inject
    private SecretRotationSaltService secretRotationSaltService;

    @Override
    public Map<SecretRotationStep, RotationContext> getContexts(String resourceCrn) {
        return Map.of(CUSTOM_JOB, getCustomJobRotationContext(resourceCrn));
    }

    private CustomJobRotationContext getCustomJobRotationContext(String resourceCrn) {
        return CustomJobRotationContext.builder()
                .withResourceCrn(resourceCrn)
                .withPreValidateJob(() -> preValidateJob(resourceCrn))
                .withRotationJob(() -> rotationJob(resourceCrn))
                .withPostValidateJob(() -> postValidateJob(resourceCrn))
                .build();
    }

    private void preValidateJob(String resourceCrn) {
        StackDto stack = stackDtoService.getByCrn(resourceCrn);
        rotateSaltPasswordValidator.validateRotateSaltPassword(stack);
    }

    private void rotationJob(String resourceCrn) {
        StackDto stack = stackDtoService.getByCrn(resourceCrn);
        try {
            rotateSaltPasswordService.rotateSaltPassword(stack);
        } catch (CloudbreakOrchestratorException e) {
            throw new SecretRotationException(e);
        }
    }

    private void postValidateJob(String resourceCrn) {
        StackDto stack = stackDtoService.getByCrn(resourceCrn);
        try {
            secretRotationSaltService.validateSalt(stack);
        } catch (CloudbreakOrchestratorException e) {
            throw new SecretRotationException(e);
        }
    }

    @Override
    public SecretType getSecret() {
        return CloudbreakSecretType.SALT_PASSWORD;
    }
}
