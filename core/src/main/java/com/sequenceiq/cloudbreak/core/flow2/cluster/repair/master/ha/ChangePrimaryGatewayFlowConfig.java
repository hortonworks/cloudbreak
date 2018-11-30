package com.sequenceiq.cloudbreak.core.flow2.cluster.repair.master.ha;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.repair.master.ha.ChangePrimaryGatewayEvent.AMBARI_SERVER_STARTED;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.repair.master.ha.ChangePrimaryGatewayEvent.AMBARI_SERVER_START_FAILED;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.repair.master.ha.ChangePrimaryGatewayEvent.CHANGE_PRIMARY_GATEWAY_FAILED;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.repair.master.ha.ChangePrimaryGatewayEvent.CHANGE_PRIMARY_GATEWAY_FAILURE_HANDLED;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.repair.master.ha.ChangePrimaryGatewayEvent.CHANGE_PRIMARY_GATEWAY_FINISHED;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.repair.master.ha.ChangePrimaryGatewayEvent.CHANGE_PRIMARY_GATEWAY_FLOW_FINISHED;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.repair.master.ha.ChangePrimaryGatewayEvent.CHANGE_PRIMARY_GATEWAY_TRIGGER_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.repair.master.ha.ChangePrimaryGatewayState.CHANGE_PRIMARY_GATEWAY_FAILED_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.repair.master.ha.ChangePrimaryGatewayState.CHANGE_PRIMARY_GATEWAY_FINISHED_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.repair.master.ha.ChangePrimaryGatewayState.CHANGE_PRIMARY_GATEWAY_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.repair.master.ha.ChangePrimaryGatewayState.FINAL_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.repair.master.ha.ChangePrimaryGatewayState.INIT_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.repair.master.ha.ChangePrimaryGatewayState.WAITING_FOR_AMBARI_SERVER_STATE;

import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.core.flow2.config.AbstractFlowConfiguration;
import com.sequenceiq.cloudbreak.core.flow2.config.AbstractFlowConfiguration.Transition.Builder;

@Component
public class ChangePrimaryGatewayFlowConfig extends AbstractFlowConfiguration<ChangePrimaryGatewayState, ChangePrimaryGatewayEvent> {

    private static final List<Transition<ChangePrimaryGatewayState, ChangePrimaryGatewayEvent>> TRANSITIONS =
            new Builder<ChangePrimaryGatewayState, ChangePrimaryGatewayEvent>()
                    .defaultFailureEvent(CHANGE_PRIMARY_GATEWAY_FAILED)
                    .from(INIT_STATE).to(CHANGE_PRIMARY_GATEWAY_STATE).event(CHANGE_PRIMARY_GATEWAY_TRIGGER_EVENT)
                    .noFailureEvent()
                    .from(CHANGE_PRIMARY_GATEWAY_STATE).to(WAITING_FOR_AMBARI_SERVER_STATE).event(CHANGE_PRIMARY_GATEWAY_FINISHED)
                    .defaultFailureEvent()
                    .from(WAITING_FOR_AMBARI_SERVER_STATE).to(CHANGE_PRIMARY_GATEWAY_FINISHED_STATE).event(AMBARI_SERVER_STARTED)
                    .failureEvent(AMBARI_SERVER_START_FAILED)
                    .from(CHANGE_PRIMARY_GATEWAY_FINISHED_STATE).to(FINAL_STATE).event(CHANGE_PRIMARY_GATEWAY_FLOW_FINISHED)
                    .defaultFailureEvent()
                    .build();

    private static final FlowEdgeConfig<ChangePrimaryGatewayState, ChangePrimaryGatewayEvent> EDGE_CONFIG = new FlowEdgeConfig<>(INIT_STATE, FINAL_STATE,
            CHANGE_PRIMARY_GATEWAY_FAILED_STATE, CHANGE_PRIMARY_GATEWAY_FAILURE_HANDLED);

    public ChangePrimaryGatewayFlowConfig() {
        super(ChangePrimaryGatewayState.class, ChangePrimaryGatewayEvent.class);
    }

    @Override
    protected List<Transition<ChangePrimaryGatewayState, ChangePrimaryGatewayEvent>> getTransitions() {
        return TRANSITIONS;
    }

    @Override
    protected FlowEdgeConfig<ChangePrimaryGatewayState, ChangePrimaryGatewayEvent> getEdgeConfig() {
        return EDGE_CONFIG;
    }

    @Override
    public ChangePrimaryGatewayEvent[] getEvents() {
        return ChangePrimaryGatewayEvent.values();
    }

    @Override
    public ChangePrimaryGatewayEvent[] getInitEvents() {
        return new ChangePrimaryGatewayEvent[]{CHANGE_PRIMARY_GATEWAY_TRIGGER_EVENT};
    }
}
