package com.sequenceiq.cloudbreak.rotation.service;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.sequenceiq.cloudbreak.rotation.CommonSecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.RotationFlowExecutionType;
import com.sequenceiq.cloudbreak.rotation.SecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.SecretType;
import com.sequenceiq.cloudbreak.rotation.common.RotationContext;
import com.sequenceiq.cloudbreak.rotation.common.RotationContextProvider;
import com.sequenceiq.cloudbreak.rotation.executor.AbstractRotationExecutor;
import com.sequenceiq.cloudbreak.rotation.vault.VaultRotationContext;
import com.sequenceiq.cloudbreak.vault.ThreadBasedVaultReadFieldProvider;

@Service
public class SecretRotationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SecretRotationService.class);

    @Inject
    private Map<SecretRotationStep, AbstractRotationExecutor<? extends RotationContext>> rotationExecutorMap;

    @Inject
    private Map<SecretType, RotationContextProvider> rotationContextProviderMap;

    @Inject
    private SecretRotationProgressService secretRotationProgressService;

    public void executePreValidation(SecretType secretType, String resourceId, RotationFlowExecutionType executionType) {
        if (executionNeeded(executionType, RotationFlowExecutionType.ROTATE, resourceId, secretType)) {
            Map<SecretRotationStep, ? extends RotationContext> contexts = getContexts(secretType, resourceId);
            LOGGER.info("Contexts generation for validation before secret rotation of {} regarding resource {} is finished.", secretType, resourceId);
            secretType.getSteps().forEach(step -> {
                LOGGER.info("Executing pre validation step {} for secret {} regarding resource {}", step, secretType, resourceId);
                rotationExecutorMap.get(step).executePreValidation(contexts.get(step));
            });
        }
    }

    public void executeRotation(SecretType secretType, String resourceId, RotationFlowExecutionType executionType) {
        if (executionNeeded(executionType, RotationFlowExecutionType.ROTATE, resourceId, secretType)) {
            Map<SecretRotationStep, ? extends RotationContext> contexts = getContexts(secretType, resourceId);
            LOGGER.info("Contexts generation for secret rotation of {} regarding resource {} is finished.", secretType, resourceId);
            secretType.getSteps().forEach(step -> {
                LOGGER.info("Executing rotation step {} for secret {} regarding resource {}", step, secretType, resourceId);
                rotationExecutorMap.get(step).executeRotate(contexts.get(step), secretType);
            });
        }
    }

    public void rollbackRotation(SecretType secretType, String resourceId, RotationFlowExecutionType executionType, SecretRotationStep failedStep) {
        if (executionNeeded(executionType, RotationFlowExecutionType.ROLLBACK, resourceId, secretType)) {
            Map<SecretRotationStep, ? extends RotationContext> contexts = getContexts(secretType, resourceId);
            LOGGER.info("Contexts generation for secret rotation's rollback of {} regarding resource {} is finished.", secretType, resourceId);
            Set<String> affectedSecrets = contexts.entrySet().stream()
                    .filter(entry -> CommonSecretRotationStep.VAULT.equals(entry.getKey()))
                    .map(Map.Entry::getValue)
                    .map(context -> ((VaultRotationContext) context).getVaultPathSecretMap().keySet())
                    .flatMap(Collection::stream)
                    .collect(Collectors.toSet());
            List<SecretRotationStep> steps = secretType.getSteps();
            List<SecretRotationStep> reversedSteps;
            if (failedStep != null) {
                LOGGER.info("Rollback of secret rotation is starting from step {}, since rotation is failed there (secret is {}, resource is {})",
                        failedStep, secretType, resourceId);
                reversedSteps = Lists.reverse(steps.subList(0, steps.indexOf(failedStep) + 1));
            } else {
                reversedSteps = Lists.reverse(steps);
            }

            reversedSteps.forEach(step -> {
                RotationContext rotationContext = contexts.get(step);
                LOGGER.info("Rolling back rotation step {} for secret {} regarding resource {}.", step, secretType, resourceId);
                LOGGER.trace("Affected secrets of rotation's rollback: {}", Joiner.on(",").join(affectedSecrets));
                ThreadBasedVaultReadFieldProvider.doRollback(affectedSecrets, () ->
                        rotationExecutorMap.get(step).executeRollback(rotationContext, secretType));
            });
        }
    }

    public void finalizeRotation(SecretType secretType, String resourceId, RotationFlowExecutionType executionType) {
        if (executionNeeded(executionType, RotationFlowExecutionType.FINALIZE, resourceId, secretType)) {
            Map<SecretRotationStep, ? extends RotationContext> contexts = getContexts(secretType, resourceId);
            LOGGER.info("Contexts generation for secret rotation's post validation and finalization of {} regarding resource {} is finished.",
                    secretType, resourceId);
            Lists.reverse(secretType.getSteps()).forEach(step -> {
                LOGGER.info("Post validating rotation step {} for secret {} regarding resource {}.", step, secretType, resourceId);
                rotationExecutorMap.get(step).executePostValidation(contexts.get(step));
            });
            Lists.reverse(secretType.getSteps()).forEach(step -> {
                LOGGER.info("Finalizing rotation step {} for secret {} regarding resource {}.", step, secretType, resourceId);
                rotationExecutorMap.get(step).executeFinalize(contexts.get(step), secretType);
            });
            secretRotationProgressService.deleteAll(resourceId, secretType);
        }
    }

    private Map<SecretRotationStep, ? extends RotationContext> getContexts(SecretType secretType, String resourceId) {
        Map<SecretRotationStep, ? extends RotationContext> contexts = rotationContextProviderMap.get(secretType).getContexts(resourceId);
        if (!contexts.keySet().containsAll(secretType.getSteps())) {
            throw new RuntimeException("At least one context is missing thus secret rotation flow step cannot be executed.");
        }
        return contexts;
    }

    private boolean executionNeeded(RotationFlowExecutionType inputExecutionType, RotationFlowExecutionType expectedExecutionType,
            String resourceId, SecretType secretType) {
        if (inputExecutionType == null || expectedExecutionType.equals(inputExecutionType)) {
            return true;
        }
        LOGGER.info("Execution of {} is not needed for resource {} regarding secret {}, skipping.",
                expectedExecutionType, resourceId, secretType);
        return false;
    }
}