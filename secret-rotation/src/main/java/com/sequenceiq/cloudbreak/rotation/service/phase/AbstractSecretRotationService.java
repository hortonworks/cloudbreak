package com.sequenceiq.cloudbreak.rotation.service.phase;

import java.util.Map;

import javax.inject.Inject;

import com.sequenceiq.cloudbreak.rotation.SecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.SecretType;
import com.sequenceiq.cloudbreak.rotation.common.RotationContext;
import com.sequenceiq.cloudbreak.rotation.common.RotationContextProvider;
import com.sequenceiq.cloudbreak.rotation.executor.AbstractRotationExecutor;
import com.sequenceiq.cloudbreak.rotation.service.RotationMetadata;

abstract class AbstractSecretRotationService {

    @Inject
    private Map<SecretRotationStep, AbstractRotationExecutor<? extends RotationContext>> rotationExecutorMap;

    @Inject
    private Map<SecretType, RotationContextProvider> rotationContextProviderMap;

    Map<SecretRotationStep, ? extends RotationContext> getContexts(SecretType secretType, String resourceCrn) {
        Map<SecretRotationStep, ? extends RotationContext> contexts = rotationContextProviderMap.get(secretType).getContexts(resourceCrn);
        if (!contexts.keySet().containsAll(secretType.getSteps())) {
            throw new RuntimeException("At least one context is missing thus secret rotation flow step cannot be executed.");
        }
        return contexts;
    }

    AbstractRotationExecutor getExecutor(SecretRotationStep step) {
        return rotationExecutorMap.get(step);
    }

    RotationContextProvider getContextProvider(RotationMetadata metadata) {
        return rotationContextProviderMap.get(metadata.secretType());
    }
}
