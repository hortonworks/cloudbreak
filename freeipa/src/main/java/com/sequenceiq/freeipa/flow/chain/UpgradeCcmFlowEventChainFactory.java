package com.sequenceiq.freeipa.flow.chain;

import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPFreeIPAStatus.Value.CCM_UPGRADE_FAILED;
import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPFreeIPAStatus.Value.CCM_UPGRADE_FINISHED;
import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPFreeIPAStatus.Value.CCM_UPGRADE_STARTED;
import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPFreeIPAStatus.Value.UNSET;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.common.usage.UsageProto.CDPFreeIPAStatus.Value;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.structuredevent.service.telemetry.mapper.FreeIpaUseCaseAware;
import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.chain.FlowEventChainFactory;
import com.sequenceiq.flow.core.chain.config.FlowTriggerEventQueue;
import com.sequenceiq.freeipa.flow.stack.update.UpdateUserDataEvents;
import com.sequenceiq.freeipa.flow.stack.update.UpdateUserDataState;
import com.sequenceiq.freeipa.flow.stack.update.event.UserDataUpdateRequest;
import com.sequenceiq.freeipa.flow.stack.upgrade.ccm.UpgradeCcmState;
import com.sequenceiq.freeipa.flow.stack.upgrade.ccm.event.UpgradeCcmFlowChainTriggerEvent;
import com.sequenceiq.freeipa.flow.stack.upgrade.ccm.event.UpgradeCcmTriggerEvent;
import com.sequenceiq.freeipa.flow.stack.upgrade.ccm.selector.UpgradeCcmStateSelector;

@Component
public class UpgradeCcmFlowEventChainFactory implements FlowEventChainFactory<UpgradeCcmFlowChainTriggerEvent>, FreeIpaUseCaseAware {

    @Override
    public String initEvent() {
        return FlowChainTriggers.UPGRADE_CCM_CHAIN_TRIGGER_EVENT;
    }

    @Override
    public FlowTriggerEventQueue createFlowTriggerEventQueue(UpgradeCcmFlowChainTriggerEvent event) {
        Queue<Selectable> flowEventChain = new ConcurrentLinkedQueue<>();
        flowEventChain.add(new UpgradeCcmTriggerEvent(UpgradeCcmStateSelector.UPGRADE_CCM_TRIGGER_EVENT.event(), event.getOperationId(), event.getResourceId(),
                event.getOldTunnel(), event.accepted())
                .withIsChained(true)
                .withIsFinal(false));
        flowEventChain.add(
                UserDataUpdateRequest.builder()
                        .withSelector(UpdateUserDataEvents.UPDATE_USERDATA_TRIGGER_EVENT.event())
                        .withStackId(event.getResourceId())
                        .withOldTunnel(event.getOldTunnel())
                        .withOperationId(event.getOperationId())
                        .withChained(true)
                        .withFinalFlow(true)
                        .build());
        return new FlowTriggerEventQueue(getName(), event, flowEventChain);
    }

    @Override
    public Value getUseCaseForFlowState(Enum<? extends FlowState> flowState) {
        if (UpgradeCcmState.INIT_STATE.equals(flowState)) {
            return CCM_UPGRADE_STARTED;
        } else if (UpdateUserDataState.UPDATE_USERDATA_FINISHED_STATE.equals(flowState)) {
            return CCM_UPGRADE_FINISHED;
        } else if (flowState.toString().contains("_FAIL")) {
            return CCM_UPGRADE_FAILED;
        } else {
            return UNSET;
        }
    }
}
