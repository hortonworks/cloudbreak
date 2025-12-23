package com.sequenceiq.cloudbreak.rotation.flow.chain;

import java.util.Set;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.NotImplementedException;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.rotation.SecretType;
import com.sequenceiq.cloudbreak.rotation.SecretTypeFlag;

public interface SecretRotationFlowEventProvider {

    default boolean saltUpdateNeeded(SecretRotationFlowChainTriggerEvent event) {
        return event.getExecutionType() == null && event.getSecretTypes().stream().anyMatch(SecretType::saltUpdateNeeded);
    }

    default boolean skipSaltHighstate(SecretRotationFlowChainTriggerEvent event) {
        return CollectionUtils.emptyIfNull(event.getSecretTypes()).stream().allMatch(SecretType::skipSaltHighstate);
    }

    Selectable getSaltUpdateTriggerEvent(SecretRotationFlowChainTriggerEvent event);

    default Set<Selectable> getPostFlowEvent(SecretRotationFlowChainTriggerEvent event) {
        if (event.getSecretTypes().stream().noneMatch(secretType -> secretType.getFlags().contains(SecretTypeFlag.POST_FLOW))) {
            return Set.of();
        }
        throw new NotImplementedException("Secret rotation cannot be initialized, please check internal logs.");
    }
}
