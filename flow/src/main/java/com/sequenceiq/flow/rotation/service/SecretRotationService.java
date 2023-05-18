package com.sequenceiq.flow.rotation.service;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.google.common.collect.Lists;
import com.sequenceiq.cloudbreak.rotation.secret.RotationContext;
import com.sequenceiq.cloudbreak.rotation.secret.RotationContextProvider;
import com.sequenceiq.cloudbreak.rotation.secret.RotationExecutor;
import com.sequenceiq.cloudbreak.rotation.secret.RotationFlowExecutionType;
import com.sequenceiq.cloudbreak.rotation.secret.SecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.secret.SecretType;
import com.sequenceiq.cloudbreak.rotation.secret.vault.VaultRotationContext;
import com.sequenceiq.cloudbreak.vault.ThreadBasedVaultReadFieldProvider;

@Service
public class SecretRotationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SecretRotationService.class);

    @Inject
    private Map<SecretRotationStep, RotationExecutor<? extends RotationContext>> rotationExecutorMap;

    @Inject
    private Map<SecretType, RotationContextProvider> rotationContextProviderMap;

    public void executeRotation(SecretType secretType, String resourceId, RotationFlowExecutionType executionType) {
        if (executionNeeded(executionType, RotationFlowExecutionType.ROTATE, resourceId, secretType)) {
            Map<SecretRotationStep, ? extends RotationContext> contexts = getContexts(secretType, resourceId);
            secretType.getSteps().forEach(step -> rotationExecutorMap.get(step).executeRotate(contexts.get(step)));
        }
    }

    public void rollbackRotation(SecretType secretType, String resourceId, RotationFlowExecutionType executionType, SecretRotationStep failedStep) {
        if (executionNeeded(executionType, RotationFlowExecutionType.ROLLBACK, resourceId, secretType)) {
            Map<SecretRotationStep, ? extends RotationContext> contexts = getContexts(secretType, resourceId);
            Set<String> affectedSecrets = contexts.entrySet().stream()
                    .filter(entry -> SecretRotationStep.VAULT.equals(entry.getKey()))
                    .map(Map.Entry::getValue)
                    .map(context -> ((VaultRotationContext) context).getVaultPathSecretMap().keySet())
                    .flatMap(Collection::stream)
                    .collect(Collectors.toSet());
            List<SecretRotationStep> steps = secretType.getSteps();
            List<SecretRotationStep> reversedSteps;
            if (failedStep != null) {
                reversedSteps = Lists.reverse(steps.subList(0, steps.indexOf(failedStep) + 1));
            } else {
                reversedSteps = Lists.reverse(steps);
            }

            reversedSteps.forEach(step -> {
                RotationContext rotationContext = contexts.get(step);
                ThreadBasedVaultReadFieldProvider.doRollback(affectedSecrets, () -> rotationExecutorMap.get(step).executeRollback(rotationContext));
            });
        }
    }

    public void finalizeRotation(SecretType secretType, String resourceId, RotationFlowExecutionType executionType) {
        if (executionNeeded(executionType, RotationFlowExecutionType.FINALIZE, resourceId, secretType)) {
            Map<SecretRotationStep, ? extends RotationContext> contexts = getContexts(secretType, resourceId);
            Lists.reverse(secretType.getSteps()).forEach(step -> rotationExecutorMap.get(step).executeFinalize(contexts.get(step)));
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
