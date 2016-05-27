package com.sequenceiq.cloudbreak.core.flow2.stack.updatesubnet;

import static com.sequenceiq.cloudbreak.core.flow2.stack.updatesubnet.UpdateSubnetEvent.FAILURE_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.updatesubnet.UpdateSubnetEvent.FAIL_HANDLED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.updatesubnet.UpdateSubnetEvent.FINALIZED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.updatesubnet.UpdateSubnetEvent.UPDATE_SUBNET_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.updatesubnet.UpdateSubnetEvent.UPDATE_SUBNET_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.updatesubnet.UpdateSubnetState.FINAL_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.stack.updatesubnet.UpdateSubnetState.INIT_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.stack.updatesubnet.UpdateSubnetState.UPDATE_SUBNET_FAILED_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.stack.updatesubnet.UpdateSubnetState.UPDATE_SUBNET_FINISHED_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.stack.updatesubnet.UpdateSubnetState.UPDATE_SUBNET_STATE;

import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.core.flow2.config.AbstractFlowConfiguration;

@Component
public class UpdateSubnetFlowConfig extends AbstractFlowConfiguration<UpdateSubnetState, UpdateSubnetEvent> {

    private static final List<Transition<UpdateSubnetState, UpdateSubnetEvent>> TRANSITIONS =
            new Transition.Builder<UpdateSubnetState, UpdateSubnetEvent>()
                    .defaultFailureEvent(FAILURE_EVENT)
                    .from(INIT_STATE).to(UPDATE_SUBNET_STATE).event(UPDATE_SUBNET_EVENT).noFailureEvent()
                    .from(UPDATE_SUBNET_STATE).to(UPDATE_SUBNET_FINISHED_STATE).event(UPDATE_SUBNET_FINISHED_EVENT).defaultFailureEvent()
                    .from(UPDATE_SUBNET_FINISHED_STATE).to(FINAL_STATE).event(FINALIZED_EVENT).defaultFailureEvent()
                    .build();

    private static final FlowEdgeConfig<UpdateSubnetState, UpdateSubnetEvent> EDGE_CONFIG = new FlowEdgeConfig<>(INIT_STATE, FINAL_STATE,
            UPDATE_SUBNET_FAILED_STATE, FAIL_HANDLED_EVENT);

    public UpdateSubnetFlowConfig() {
        super(UpdateSubnetState.class, UpdateSubnetEvent.class);
    }

    @Override
    protected List<Transition<UpdateSubnetState, UpdateSubnetEvent>> getTransitions() {
        return TRANSITIONS;
    }

    @Override
    protected FlowEdgeConfig<UpdateSubnetState, UpdateSubnetEvent> getEdgeConfig() {
        return EDGE_CONFIG;
    }

    @Override
    public UpdateSubnetEvent[] getEvents() {
        return UpdateSubnetEvent.values();
    }

    @Override
    public UpdateSubnetEvent[] getInitEvents() {
        return new UpdateSubnetEvent[] {
                UpdateSubnetEvent.UPDATE_SUBNET_EVENT
        };
    }
}
