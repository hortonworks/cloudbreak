package com.sequenceiq.cloudbreak.rotation.flow.chain;

import java.util.List;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.rotation.RotationFlowExecutionType;
import com.sequenceiq.cloudbreak.rotation.SecretType;

public interface SaltUpdateFlowEventProvider {

    default boolean saltUpdateNeeded(List<SecretType> secretTypes, RotationFlowExecutionType requestedExecution) {
        return requestedExecution == null && secretTypes.stream().anyMatch(SecretType::saltUpdateNeeded);
    }

    Selectable getSaltUpdateTriggerEvent(SecretRotationFlowChainTriggerEvent event);
}
