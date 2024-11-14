package com.sequenceiq.cloudbreak.rotation.flow.chain;

import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import jakarta.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.rotation.SecretType;
import com.sequenceiq.cloudbreak.rotation.flow.rotation.event.SecretRotationTriggerEvent;
import com.sequenceiq.cloudbreak.rotation.flow.subrotation.event.SecretSubRotationTriggerEvent;
import com.sequenceiq.flow.core.chain.FlowEventChainFactory;
import com.sequenceiq.flow.core.chain.config.FlowTriggerEventQueue;
import com.sequenceiq.flow.event.EventSelectorUtil;

@Component
public class SecretRotationFlowEventChainFactory implements FlowEventChainFactory<SecretRotationFlowChainTriggerEvent> {

    @Inject
    private Optional<SecretRotationFlowEventProvider> secretRotationFlowEventProviderOptional;

    @Override
    public String initEvent() {
        return EventSelectorUtil.selector(SecretRotationFlowChainTriggerEvent.class);
    }

    @Override
    public FlowTriggerEventQueue createFlowTriggerEventQueue(SecretRotationFlowChainTriggerEvent event) {
        Queue<Selectable> flowEventChain = new ConcurrentLinkedQueue<>();
        secretRotationFlowEventProviderOptional.stream()
                .filter(flowEventProvider -> flowEventProvider.saltUpdateNeeded(event))
                .forEach(flowEventProvider -> flowEventChain.add(flowEventProvider.getSaltUpdateTriggerEvent(event)));
        event.getSecretTypes().forEach(secretType -> flowEventChain.add(getSecretRotationFlowTriggerEvent(event, secretType)));
        secretRotationFlowEventProviderOptional.ifPresent(flowEventProvider -> flowEventChain.addAll(flowEventProvider.getPostFlowEvent(event)));
        return new FlowTriggerEventQueue(getName(), event, flowEventChain);
    }

    private static Selectable getSecretRotationFlowTriggerEvent(SecretRotationFlowChainTriggerEvent event, SecretType secretType) {
        if (event.getExecutionType() == null) {
            return SecretRotationTriggerEvent.fromChainTrigger(event, secretType);
        } else {
            return SecretSubRotationTriggerEvent.fromChainTrigger(event, secretType);
        }
    }
}
