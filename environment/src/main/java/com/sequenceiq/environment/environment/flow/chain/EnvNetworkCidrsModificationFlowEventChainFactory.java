package com.sequenceiq.environment.environment.flow.chain;

import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPEnvironmentStatus.Value;
import static com.sequenceiq.environment.environment.flow.modify.network.event.EnvNetworkCidrsModificationStateSelectors.START_MODIFY_ENVIRONMENT_NETWORK_CIDRS_EVENT;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.structuredevent.service.telemetry.mapper.EnvironmentUseCaseAware;
import com.sequenceiq.environment.environment.flow.config.update.EnvStackConfigUpdatesState;
import com.sequenceiq.environment.environment.flow.config.update.event.EnvStackConfigUpdatesEvent;
import com.sequenceiq.environment.environment.flow.config.update.event.EnvStackConfigUpdatesStateSelectors;
import com.sequenceiq.environment.environment.flow.modify.network.EnvNetworkCidrsModificationState;
import com.sequenceiq.environment.environment.flow.modify.network.event.EnvNetworkCidrsModificationEvent;
import com.sequenceiq.environment.environment.flow.modify.network.event.EnvNetworkCidrsModificationTriggerEvent;
import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.chain.FlowEventChainFactory;
import com.sequenceiq.flow.core.chain.config.FlowTriggerEventQueue;

@Component
public class EnvNetworkCidrsModificationFlowEventChainFactory
        implements FlowEventChainFactory<EnvNetworkCidrsModificationTriggerEvent>, EnvironmentUseCaseAware {

    @Override
    public Value getUseCaseForFlowState(Enum<? extends FlowState> flowState) {
        return switch (flowState) {
            case EnvNetworkCidrsModificationState s when s == EnvNetworkCidrsModificationState.INIT_STATE -> Value.NETWORK_CIDRS_MODIFICATION_STARTED;
            case EnvStackConfigUpdatesState s when s == EnvStackConfigUpdatesState.FINAL_STATE -> Value.NETWORK_CIDRS_MODIFICATION_FINISHED;
            case EnvNetworkCidrsModificationState s when s == EnvNetworkCidrsModificationState.NETWORK_CIDRS_MODIFICATION_FAILED_STATE ->
                    Value.NETWORK_CIDRS_MODIFICATION_FAILED;
            case EnvStackConfigUpdatesState s when s == EnvStackConfigUpdatesState.STACK_CONFIG_UPDATES_FAILED_STATE -> Value.NETWORK_CIDRS_MODIFICATION_FAILED;
            default -> Value.UNSET;
        };
    }

    @Override
    public String initEvent() {
        return FlowChainTriggers.ENV_MODIFY_NETWORK_CIDRS_TRIGGER_EVENT;
    }

    @Override
    public FlowTriggerEventQueue createFlowTriggerEventQueue(EnvNetworkCidrsModificationTriggerEvent event) {
        Queue<Selectable> flowEventChain = new ConcurrentLinkedQueue<>();

        flowEventChain.add(getEnvNetworkCidrsModificationEvent(event));
        flowEventChain.add(getEnvConfigUpdatesEvent(event));

        return new FlowTriggerEventQueue(getName(), event, flowEventChain);
    }

    private EnvNetworkCidrsModificationEvent getEnvNetworkCidrsModificationEvent(EnvNetworkCidrsModificationTriggerEvent event) {
        return EnvNetworkCidrsModificationEvent.builder()
                .withAccepted(event.accepted())
                .withSelector(START_MODIFY_ENVIRONMENT_NETWORK_CIDRS_EVENT.selector())
                .withResourceId(event.getResourceId())
                .withResourceName(event.getResourceName())
                .withResourceCrn(event.getResourceCrn())
                .withNetworkCidrs(event.getNetworkCidrs())
                .build();
    }

    private EnvStackConfigUpdatesEvent getEnvConfigUpdatesEvent(EnvNetworkCidrsModificationTriggerEvent event) {
        return EnvStackConfigUpdatesEvent.Builder
                .anEnvStackConfigUpdatesEvent()
                .withAccepted(event.accepted())
                .withSelector(EnvStackConfigUpdatesStateSelectors.ENV_STACK_CONFIG_UPDATES_START_EVENT.selector())
                .withResourceId(event.getResourceId())
                .withResourceName(event.getResourceName())
                .withResourceCrn(event.getResourceCrn())
                .build();
    }
}
