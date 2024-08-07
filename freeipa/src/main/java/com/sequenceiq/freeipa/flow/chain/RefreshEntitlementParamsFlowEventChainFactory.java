package com.sequenceiq.freeipa.flow.chain;


import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPFreeIPAStatus.Value.ENTITLEMENT_SYNC_FAILED;
import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPFreeIPAStatus.Value.ENTITLEMENT_SYNC_FINISHED;
import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPFreeIPAStatus.Value.ENTITLEMENT_SYNC_STARTED;
import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPFreeIPAStatus.Value.UNSET;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.common.usage.UsageProto.CDPFreeIPAStatus.Value;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.structuredevent.service.telemetry.mapper.FreeIpaUseCaseAware;
import com.sequenceiq.flow.core.chain.FlowEventChainFactory;
import com.sequenceiq.flow.core.chain.config.FlowTriggerEventQueue;
import com.sequenceiq.flow.core.chain.finalize.config.FlowChainFinalizeState;
import com.sequenceiq.flow.core.chain.finalize.flowevents.FlowChainFinalizePayload;
import com.sequenceiq.flow.core.chain.init.config.FlowChainInitState;
import com.sequenceiq.flow.core.chain.init.flowevents.FlowChainInitPayload;
import com.sequenceiq.freeipa.flow.freeipa.salt.update.SaltUpdateTriggerEvent;
import com.sequenceiq.freeipa.flow.stack.dynamicentitlement.RefreshEntitlementParamsFlowChainTriggerEvent;
import com.sequenceiq.freeipa.flow.stack.dynamicentitlement.RefreshEntitlementParamsTriggerEvent;

@Component
public class RefreshEntitlementParamsFlowEventChainFactory implements FlowEventChainFactory<RefreshEntitlementParamsFlowChainTriggerEvent>, FreeIpaUseCaseAware {

    @Override
    public String initEvent() {
        return FlowChainTriggers.REFRESH_ENTITLEMENT_PARAM_CHAIN_TRIGGER_EVENT;
    }

    @Override
    public FlowTriggerEventQueue createFlowTriggerEventQueue(RefreshEntitlementParamsFlowChainTriggerEvent event) {
        Queue<Selectable> flowEventChain = new ConcurrentLinkedQueue<>();
        flowEventChain.add(new FlowChainInitPayload(getName(), event.getResourceId(), event.accepted()));
        if (event.getSaltRefreshNeeded()) {
            flowEventChain.add(new SaltUpdateTriggerEvent(event.getResourceId(),
                    event.accepted(), true, false, event.getOperationId()));
        }
        flowEventChain.add(RefreshEntitlementParamsTriggerEvent.fromChainTrigger(event, true, true));
        flowEventChain.add(new FlowChainFinalizePayload(getName(), event.getResourceId(), event.accepted()));
        return new FlowTriggerEventQueue(getName(), event, flowEventChain);
    }

    public Value getUseCaseForFlowState(Enum flowState) {
        if (FlowChainInitState.INIT_STATE.equals(flowState)) {
            return ENTITLEMENT_SYNC_STARTED;
        } else if (FlowChainFinalizeState.FLOWCHAIN_FINALIZE_FINISHED_STATE.equals(flowState)) {
            return ENTITLEMENT_SYNC_FINISHED;
        } else if (flowState.toString().endsWith("FAILED_STATE")) {
            return ENTITLEMENT_SYNC_FAILED;
        } else {
            return UNSET;
        }
    }
}
