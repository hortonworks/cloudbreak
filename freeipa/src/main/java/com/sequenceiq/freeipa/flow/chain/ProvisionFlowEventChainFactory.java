package com.sequenceiq.freeipa.flow.chain;

import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPFreeIPAStatus.Value.CREATE_FAILED;
import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPFreeIPAStatus.Value.CREATE_FINISHED;
import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPFreeIPAStatus.Value.CREATE_STARTED;
import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPFreeIPAStatus.Value.UNSET;

import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import jakarta.inject.Inject;

import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.common.usage.UsageProto.CDPFreeIPAStatus.Value;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.structuredevent.service.telemetry.mapper.FreeIpaUseCaseAware;
import com.sequenceiq.flow.api.model.operation.OperationType;
import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.chain.FlowEventChainFactory;
import com.sequenceiq.flow.core.chain.config.FlowTriggerEventQueue;
import com.sequenceiq.freeipa.flow.freeipa.loadbalancer.FreeIpaLoadBalancerCreationEvent;
import com.sequenceiq.freeipa.flow.freeipa.provision.FreeIpaProvisionEvent;
import com.sequenceiq.freeipa.flow.freeipa.provision.FreeIpaProvisionState;
import com.sequenceiq.freeipa.flow.stack.StackEvent;
import com.sequenceiq.freeipa.flow.stack.provision.StackProvisionEvent;
import com.sequenceiq.freeipa.flow.stack.provision.StackProvisionState;
import com.sequenceiq.freeipa.util.CrnService;

@Component
public class ProvisionFlowEventChainFactory implements FlowEventChainFactory<StackEvent>, FreeIpaUseCaseAware {

    @Inject
    private EntitlementService entitlementService;

    @Inject
    private CrnService crnService;

    @Override
    public String initEvent() {
        return FlowChainTriggers.PROVISION_TRIGGER_EVENT;
    }

    @Override
    public FlowTriggerEventQueue createFlowTriggerEventQueue(StackEvent event) {
        Queue<Selectable> flowEventChain = new ConcurrentLinkedQueue<>();
        flowEventChain.add(new StackEvent(StackProvisionEvent.START_CREATION_EVENT.event(), event.getResourceId(), event.accepted()));
        createLoadBalancerCreationFlowIfNecessary(event).ifPresent(flowEventChain::add);
        flowEventChain.add(new StackEvent(FreeIpaProvisionEvent.FREEIPA_PROVISION_EVENT.event(), event.getResourceId()));
        return new FlowTriggerEventQueue(getName(), event, flowEventChain);
    }

    @Override
    public OperationType getFlowOperationType() {
        return OperationType.PROVISION;
    }

    private Optional<StackEvent> createLoadBalancerCreationFlowIfNecessary(StackEvent event) {
        String accountId = crnService.getCurrentAccountId();
        if (entitlementService.isFreeIpaLoadBalancerEnabled(accountId)) {
            return Optional.of(new StackEvent(FreeIpaLoadBalancerCreationEvent.FREEIPA_LOAD_BALANCER_CREATION_EVENT.event(), event.getResourceId()));
        } else {
            return Optional.empty();
        }
    }

    @Override
    public Value getUseCaseForFlowState(Enum<? extends FlowState> flowState) {
        if (StackProvisionState.INIT_STATE.equals(flowState)) {
            return CREATE_STARTED;
        } else if (FreeIpaProvisionState.FREEIPA_PROVISION_FINISHED_STATE.equals(flowState)) {
            return CREATE_FINISHED;
        } else if (flowState.toString().endsWith("FAILED_STATE")) {
            return CREATE_FAILED;
        } else {
            return UNSET;
        }
    }
}