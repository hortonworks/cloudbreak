package com.sequenceiq.cloudbreak.core.flow2.chain;

import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPClusterStatus.Value.STOP_START_DOWNSCALE_FAILED;
import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPClusterStatus.Value.STOP_START_DOWNSCALE_FINISHED;
import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPClusterStatus.Value.STOP_START_DOWNSCALE_STARTED;
import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPClusterStatus.Value.UNSET;
import static com.sequenceiq.cloudbreak.core.flow2.stack.sync.StackSyncEvent.STACK_SYNC_EVENT;

import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

import jakarta.inject.Inject;

import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.common.usage.UsageProto.CDPClusterStatus.Value;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.core.flow2.cluster.stopstartds.StopStartDownscaleEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.stopstartds.StopStartDownscaleState;
import com.sequenceiq.cloudbreak.core.flow2.event.ClusterAndStackDownscaleTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.event.StackSyncTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.event.StopStartDownscaleTriggerEvent;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.structuredevent.service.telemetry.mapper.ClusterUseCaseAware;
import com.sequenceiq.cloudbreak.util.StackUtil;
import com.sequenceiq.cloudbreak.view.StackView;
import com.sequenceiq.flow.core.chain.FlowEventChainFactory;
import com.sequenceiq.flow.core.chain.config.FlowTriggerEventQueue;

@Component
public class StopStartDownscaleFlowEventChainFactory implements FlowEventChainFactory<ClusterAndStackDownscaleTriggerEvent>, ClusterUseCaseAware {

    @Inject
    private StackDtoService stackDtoService;

    @Inject
    private StackUtil stackUtil;

    @Override
    public String initEvent() {
        return FlowChainTriggers.STOPSTART_DOWNSCALE_CHAIN_TRIGGER_EVENT;
    }

    @Override
    public FlowTriggerEventQueue createFlowTriggerEventQueue(ClusterAndStackDownscaleTriggerEvent event) {

        StackView stackView = stackDtoService.getStackViewById(event.getResourceId());
        Map<String, Set<Long>> hostGroupsWithPrivateIds = event.getHostGroupsWithPrivateIds();

        Queue<Selectable> flowEventChain = new ConcurrentLinkedQueue<>();

        // TODO CB-14929: Is a stack sync really required here. What does it do ? (As of now it also serves to accept the event)
        addStackSyncTriggerEvent(event, flowEventChain);

        if (hostGroupsWithPrivateIds.keySet().size() > 1) {
            throw new BadRequestException("Start stop downscale flow was intended to handle only 1 hostgroup.");
        }
        for (Map.Entry<String, Set<Long>> hostGroupWithPrivateIds : hostGroupsWithPrivateIds.entrySet()) {
            StopStartDownscaleTriggerEvent te = new StopStartDownscaleTriggerEvent(
                    StopStartDownscaleEvent.STOPSTART_DOWNSCALE_TRIGGER_EVENT.event(),
                    stackView.getId(),
                    hostGroupWithPrivateIds.getKey(),
                    hostGroupWithPrivateIds.getValue(),
                    stackUtil.stopStartScalingFailureRecoveryEnabled(stackView)
            );
            flowEventChain.add(te);
        }

        return new FlowTriggerEventQueue(getName(), event, flowEventChain);
    }

    @Override
    public Value getUseCaseForFlowState(Enum flowState) {
        if (StopStartDownscaleState.INIT_STATE.equals(flowState)) {
            return STOP_START_DOWNSCALE_STARTED;
        } else if (StopStartDownscaleState.STOPSTART_DOWNSCALE_FINALIZE_STATE.equals(flowState)) {
            return STOP_START_DOWNSCALE_FINISHED;
        } else if (flowState.toString().endsWith("FAILED_STATE") &&
                !StopStartDownscaleState.STOPSTART_DOWNSCALE_STOP_INSTANCES_FAILED_STATE.equals(flowState) &&
                !StopStartDownscaleState.STOPSTART_DOWNSCALE_DECOMMISSION_VIA_CM_FAILED_STATE.equals(flowState)) {
            return STOP_START_DOWNSCALE_FAILED;
        } else {
            return UNSET;
        }
    }

    private void addStackSyncTriggerEvent(ClusterAndStackDownscaleTriggerEvent event, Queue<Selectable> flowEventChain) {
        flowEventChain.add(new StackSyncTriggerEvent(
                STACK_SYNC_EVENT.event(),
                event.getResourceId(),
                false,
                event.accepted())
        );
    }
}
