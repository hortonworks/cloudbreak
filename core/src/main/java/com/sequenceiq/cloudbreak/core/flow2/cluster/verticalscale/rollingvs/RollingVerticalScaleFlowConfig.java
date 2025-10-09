package com.sequenceiq.cloudbreak.core.flow2.cluster.verticalscale.rollingvs;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.verticalscale.rollingvs.RollingVerticalScaleEvent.ROLLING_VERTICALSCALE_FAILURE_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.verticalscale.rollingvs.RollingVerticalScaleEvent.ROLLING_VERTICALSCALE_FAIL_HANDLED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.verticalscale.rollingvs.RollingVerticalScaleEvent.ROLLING_VERTICALSCALE_FINALIZED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.verticalscale.rollingvs.RollingVerticalScaleEvent.ROLLING_VERTICALSCALE_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.verticalscale.rollingvs.RollingVerticalScaleEvent.ROLLING_VERTICALSCALE_SCALE_INSTANCES_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.verticalscale.rollingvs.RollingVerticalScaleEvent.ROLLING_VERTICALSCALE_START_INSTANCES_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.verticalscale.rollingvs.RollingVerticalScaleEvent.ROLLING_VERTICALSCALE_TRIGGER_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.verticalscale.rollingvs.RollingVerticalScaleState.FINAL_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.verticalscale.rollingvs.RollingVerticalScaleState.INIT_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.verticalscale.rollingvs.RollingVerticalScaleState.ROLLING_VERTICALSCALE_FAILED_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.verticalscale.rollingvs.RollingVerticalScaleState.ROLLING_VERTICALSCALE_FINISHED_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.verticalscale.rollingvs.RollingVerticalScaleState.ROLLING_VERTICALSCALE_SCALE_INSTANCES_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.verticalscale.rollingvs.RollingVerticalScaleState.ROLLING_VERTICALSCALE_START_INSTANCES_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.verticalscale.rollingvs.RollingVerticalScaleState.ROLLING_VERTICALSCALE_STOP_INSTANCES_STATE;

import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.core.flow2.StackStatusFinalizerAbstractFlowConfig;

@Component
public class RollingVerticalScaleFlowConfig extends StackStatusFinalizerAbstractFlowConfig<RollingVerticalScaleState, RollingVerticalScaleEvent> {

    private static final List<Transition<RollingVerticalScaleState, RollingVerticalScaleEvent>> TRANSITIONS =
            new Transition.Builder<RollingVerticalScaleState, RollingVerticalScaleEvent>()
            .defaultFailureEvent(ROLLING_VERTICALSCALE_FAILURE_EVENT)
            .from(INIT_STATE)
                    .to(ROLLING_VERTICALSCALE_STOP_INSTANCES_STATE)
                    .event(ROLLING_VERTICALSCALE_TRIGGER_EVENT)
                    .defaultFailureEvent()
            .from(ROLLING_VERTICALSCALE_STOP_INSTANCES_STATE)
                    .to(ROLLING_VERTICALSCALE_SCALE_INSTANCES_STATE)
                    .event(ROLLING_VERTICALSCALE_SCALE_INSTANCES_EVENT)
                    .defaultFailureEvent()
            .from(ROLLING_VERTICALSCALE_SCALE_INSTANCES_STATE)
                    .to(ROLLING_VERTICALSCALE_START_INSTANCES_STATE)
                    .event(ROLLING_VERTICALSCALE_START_INSTANCES_EVENT)
                    .defaultFailureEvent()
            .from(ROLLING_VERTICALSCALE_START_INSTANCES_STATE)
                    .to(ROLLING_VERTICALSCALE_FINISHED_STATE)
                    .event(ROLLING_VERTICALSCALE_FINISHED_EVENT)
                    .defaultFailureEvent()
            .from(ROLLING_VERTICALSCALE_FINISHED_STATE)
                    .to(FINAL_STATE)
                    .event(ROLLING_VERTICALSCALE_FINALIZED_EVENT)
                    .defaultFailureEvent()
            .build();

    private static final FlowEdgeConfig<RollingVerticalScaleState, RollingVerticalScaleEvent> EDGE_CONFIG =
            new FlowEdgeConfig<>(INIT_STATE, FINAL_STATE, ROLLING_VERTICALSCALE_FAILED_STATE, ROLLING_VERTICALSCALE_FAIL_HANDLED_EVENT);

    protected RollingVerticalScaleFlowConfig() {
        super(RollingVerticalScaleState.class, RollingVerticalScaleEvent.class);
    }

    @Override
    protected List<Transition<RollingVerticalScaleState, RollingVerticalScaleEvent>> getTransitions() {
        return TRANSITIONS;
    }

    @Override
    public FlowEdgeConfig<RollingVerticalScaleState, RollingVerticalScaleEvent> getEdgeConfig() {
        return EDGE_CONFIG;
    }

    @Override
    public RollingVerticalScaleEvent[] getEvents() {
        return RollingVerticalScaleEvent.values();
    }

    @Override
    public RollingVerticalScaleEvent[] getInitEvents() {
        return new RollingVerticalScaleEvent[]{ROLLING_VERTICALSCALE_TRIGGER_EVENT};
    }

    @Override
    public String getDisplayName() {
        return "Rolling Vertical Scale";
    }
}
