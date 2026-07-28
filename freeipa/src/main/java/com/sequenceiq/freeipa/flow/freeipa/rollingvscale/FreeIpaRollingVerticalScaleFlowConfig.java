package com.sequenceiq.freeipa.flow.freeipa.rollingvscale;

import static com.sequenceiq.freeipa.flow.freeipa.rollingvscale.FreeIpaRollingVerticalScaleEvent.ROLLING_VERTICAL_SCALE_DESCRIBE_FINISHED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.rollingvscale.FreeIpaRollingVerticalScaleEvent.ROLLING_VERTICAL_SCALE_DESCRIBE_SKIP_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.rollingvscale.FreeIpaRollingVerticalScaleEvent.ROLLING_VERTICAL_SCALE_FAILURE_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.rollingvscale.FreeIpaRollingVerticalScaleEvent.ROLLING_VERTICAL_SCALE_FAIL_HANDLED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.rollingvscale.FreeIpaRollingVerticalScaleEvent.ROLLING_VERTICAL_SCALE_FINALIZED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.rollingvscale.FreeIpaRollingVerticalScaleEvent.ROLLING_VERTICAL_SCALE_HEALTH_CHECK_FINISHED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.rollingvscale.FreeIpaRollingVerticalScaleEvent.ROLLING_VERTICAL_SCALE_RESIZE_FINISHED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.rollingvscale.FreeIpaRollingVerticalScaleEvent.ROLLING_VERTICAL_SCALE_START_VM_FAILURE_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.rollingvscale.FreeIpaRollingVerticalScaleEvent.ROLLING_VERTICAL_SCALE_START_VM_FINISHED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.rollingvscale.FreeIpaRollingVerticalScaleEvent.ROLLING_VERTICAL_SCALE_STOP_HEALTH_AGENT_FINISHED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.rollingvscale.FreeIpaRollingVerticalScaleEvent.ROLLING_VERTICAL_SCALE_STOP_SERVICES_FINISHED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.rollingvscale.FreeIpaRollingVerticalScaleEvent.ROLLING_VERTICAL_SCALE_STOP_VM_FAILURE_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.rollingvscale.FreeIpaRollingVerticalScaleEvent.ROLLING_VERTICAL_SCALE_STOP_VM_FINISHED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.rollingvscale.FreeIpaRollingVerticalScaleEvent.ROLLING_VERTICAL_SCALE_TRIGGER_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.rollingvscale.FreeIpaRollingVerticalScaleState.FINAL_STATE;
import static com.sequenceiq.freeipa.flow.freeipa.rollingvscale.FreeIpaRollingVerticalScaleState.INIT_STATE;
import static com.sequenceiq.freeipa.flow.freeipa.rollingvscale.FreeIpaRollingVerticalScaleState.ROLLING_VERTICAL_SCALE_DESCRIBE_STATE;
import static com.sequenceiq.freeipa.flow.freeipa.rollingvscale.FreeIpaRollingVerticalScaleState.ROLLING_VERTICAL_SCALE_FAILED_STATE;
import static com.sequenceiq.freeipa.flow.freeipa.rollingvscale.FreeIpaRollingVerticalScaleState.ROLLING_VERTICAL_SCALE_FINISHED_STATE;
import static com.sequenceiq.freeipa.flow.freeipa.rollingvscale.FreeIpaRollingVerticalScaleState.ROLLING_VERTICAL_SCALE_HEALTH_CHECK_STATE;
import static com.sequenceiq.freeipa.flow.freeipa.rollingvscale.FreeIpaRollingVerticalScaleState.ROLLING_VERTICAL_SCALE_RESIZE_STATE;
import static com.sequenceiq.freeipa.flow.freeipa.rollingvscale.FreeIpaRollingVerticalScaleState.ROLLING_VERTICAL_SCALE_START_VM_STATE;
import static com.sequenceiq.freeipa.flow.freeipa.rollingvscale.FreeIpaRollingVerticalScaleState.ROLLING_VERTICAL_SCALE_STOP_HEALTH_AGENT_STATE;
import static com.sequenceiq.freeipa.flow.freeipa.rollingvscale.FreeIpaRollingVerticalScaleState.ROLLING_VERTICAL_SCALE_STOP_SERVICES_STATE;
import static com.sequenceiq.freeipa.flow.freeipa.rollingvscale.FreeIpaRollingVerticalScaleState.ROLLING_VERTICAL_SCALE_STOP_VM_STATE;

import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.flow.core.config.AbstractFlowConfiguration.Transition.Builder;
import com.sequenceiq.freeipa.flow.StackStatusFinalizerAbstractFlowConfig;

@Component
public class FreeIpaRollingVerticalScaleFlowConfig
        extends StackStatusFinalizerAbstractFlowConfig<FreeIpaRollingVerticalScaleState, FreeIpaRollingVerticalScaleEvent> {

    private static final List<Transition<FreeIpaRollingVerticalScaleState, FreeIpaRollingVerticalScaleEvent>> TRANSITIONS =
            new Builder<FreeIpaRollingVerticalScaleState, FreeIpaRollingVerticalScaleEvent>()
                    .defaultFailureEvent(ROLLING_VERTICAL_SCALE_FAILURE_EVENT)

                    .from(INIT_STATE)
                    .to(ROLLING_VERTICAL_SCALE_DESCRIBE_STATE)
                    .event(ROLLING_VERTICAL_SCALE_TRIGGER_EVENT)
                    .noFailureEvent()

                    .from(ROLLING_VERTICAL_SCALE_DESCRIBE_STATE)
                    .to(ROLLING_VERTICAL_SCALE_STOP_HEALTH_AGENT_STATE)
                    .event(ROLLING_VERTICAL_SCALE_DESCRIBE_FINISHED_EVENT)
                    .defaultFailureEvent()

                    .from(ROLLING_VERTICAL_SCALE_DESCRIBE_STATE)
                    .to(ROLLING_VERTICAL_SCALE_FINISHED_STATE)
                    .event(ROLLING_VERTICAL_SCALE_DESCRIBE_SKIP_EVENT)
                    .defaultFailureEvent()

                    .from(ROLLING_VERTICAL_SCALE_STOP_HEALTH_AGENT_STATE)
                    .to(ROLLING_VERTICAL_SCALE_STOP_SERVICES_STATE)
                    .event(ROLLING_VERTICAL_SCALE_STOP_HEALTH_AGENT_FINISHED_EVENT)
                    .defaultFailureEvent()

                    .from(ROLLING_VERTICAL_SCALE_STOP_SERVICES_STATE)
                    .to(ROLLING_VERTICAL_SCALE_STOP_VM_STATE)
                    .event(ROLLING_VERTICAL_SCALE_STOP_SERVICES_FINISHED_EVENT)
                    .defaultFailureEvent()

                    .from(ROLLING_VERTICAL_SCALE_STOP_VM_STATE)
                    .to(ROLLING_VERTICAL_SCALE_RESIZE_STATE)
                    .event(ROLLING_VERTICAL_SCALE_STOP_VM_FINISHED_EVENT)
                    .failureEvent(ROLLING_VERTICAL_SCALE_STOP_VM_FAILURE_EVENT)

                    .from(ROLLING_VERTICAL_SCALE_RESIZE_STATE)
                    .to(ROLLING_VERTICAL_SCALE_START_VM_STATE)
                    .event(ROLLING_VERTICAL_SCALE_RESIZE_FINISHED_EVENT)
                    .defaultFailureEvent()

                    .from(ROLLING_VERTICAL_SCALE_START_VM_STATE)
                    .to(ROLLING_VERTICAL_SCALE_HEALTH_CHECK_STATE)
                    .event(ROLLING_VERTICAL_SCALE_START_VM_FINISHED_EVENT)
                    .failureEvent(ROLLING_VERTICAL_SCALE_START_VM_FAILURE_EVENT)

                    .from(ROLLING_VERTICAL_SCALE_HEALTH_CHECK_STATE)
                    .to(ROLLING_VERTICAL_SCALE_FINISHED_STATE)
                    .event(ROLLING_VERTICAL_SCALE_HEALTH_CHECK_FINISHED_EVENT)
                    .defaultFailureEvent()

                    .from(ROLLING_VERTICAL_SCALE_FINISHED_STATE)
                    .to(FINAL_STATE)
                    .event(ROLLING_VERTICAL_SCALE_FINALIZED_EVENT)
                    .defaultFailureEvent()

                    .build();

    private static final FlowEdgeConfig<FreeIpaRollingVerticalScaleState, FreeIpaRollingVerticalScaleEvent> EDGE_CONFIG = new FlowEdgeConfig<>(
            INIT_STATE,
            FINAL_STATE,
            ROLLING_VERTICAL_SCALE_FAILED_STATE,
            ROLLING_VERTICAL_SCALE_FAIL_HANDLED_EVENT);

    public FreeIpaRollingVerticalScaleFlowConfig() {
        super(FreeIpaRollingVerticalScaleState.class, FreeIpaRollingVerticalScaleEvent.class);
    }

    @Override
    protected List<Transition<FreeIpaRollingVerticalScaleState, FreeIpaRollingVerticalScaleEvent>> getTransitions() {
        return TRANSITIONS;
    }

    @Override
    public FlowEdgeConfig<FreeIpaRollingVerticalScaleState, FreeIpaRollingVerticalScaleEvent> getEdgeConfig() {
        return EDGE_CONFIG;
    }

    @Override
    public FreeIpaRollingVerticalScaleEvent[] getEvents() {
        return FreeIpaRollingVerticalScaleEvent.values();
    }

    @Override
    public FreeIpaRollingVerticalScaleEvent[] getInitEvents() {
        return new FreeIpaRollingVerticalScaleEvent[]{ROLLING_VERTICAL_SCALE_TRIGGER_EVENT};
    }

    @Override
    public String getDisplayName() {
        return "Rolling vertical scale on FreeIPA";
    }
}
