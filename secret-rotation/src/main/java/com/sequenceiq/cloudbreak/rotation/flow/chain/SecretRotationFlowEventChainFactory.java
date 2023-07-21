package com.sequenceiq.cloudbreak.rotation.flow.chain;

import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.rotation.RotationFlowExecutionType;
import com.sequenceiq.cloudbreak.rotation.flow.rotation.event.SecretRotationTriggerEvent;
import com.sequenceiq.flow.core.chain.FlowEventChainFactory;
import com.sequenceiq.flow.core.chain.config.FlowTriggerEventQueue;
import com.sequenceiq.flow.event.EventSelectorUtil;

@Component
public class SecretRotationFlowEventChainFactory implements FlowEventChainFactory<SecretRotationFlowChainTriggerEvent> {

    @Inject
    private Optional<BeforeRotationFlowEventProvider> beforeRotationFlowEventProvider;

    @Override
    public String initEvent() {
        return EventSelectorUtil.selector(SecretRotationFlowChainTriggerEvent.class);
    }

    @Override
    public FlowTriggerEventQueue createFlowTriggerEventQueue(SecretRotationFlowChainTriggerEvent event) {
        Queue<Selectable> flowEventChain = new ConcurrentLinkedQueue<>();
        if (beforeRotationFlowEventProvider.isPresent() && isRotationOrAllExecution(event)) {
            flowEventChain.add(beforeRotationFlowEventProvider.get().getTriggerEvent(event));
        }
        event.getSecretTypes().forEach(secretType -> {
            flowEventChain.add(SecretRotationTriggerEvent.fromChainTrigger(event, secretType));
        });
        return new FlowTriggerEventQueue(getName(), event, flowEventChain);
    }

    private boolean isRotationOrAllExecution(SecretRotationFlowChainTriggerEvent event) {
        return event.getExecutionType() == null || RotationFlowExecutionType.ROTATE.equals(event.getExecutionType());
    }
}
