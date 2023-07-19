package com.sequenceiq.cloudbreak.rotation.flow.chain;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.rotation.SecretType;

public interface SaltUpdateFlowEventProvider {

    default boolean saltUpdateNeeded(SecretRotationFlowChainTriggerEvent event) {
        return event.getExecutionType() == null && event.getSecretTypes().stream().anyMatch(SecretType::saltUpdateNeeded);
    }

    Selectable getSaltUpdateTriggerEvent(SecretRotationFlowChainTriggerEvent event);
}
