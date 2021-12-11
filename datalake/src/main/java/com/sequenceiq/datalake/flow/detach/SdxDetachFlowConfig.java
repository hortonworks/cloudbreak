package com.sequenceiq.datalake.flow.detach;

import static com.sequenceiq.datalake.flow.detach.SdxDetachEvent.SDX_ATTACH_NEW_CLUSTER_FAILED_EVENT;
import static com.sequenceiq.datalake.flow.detach.SdxDetachEvent.SDX_ATTACH_NEW_CLUSTER_SUCCESS_EVENT;
import static com.sequenceiq.datalake.flow.detach.SdxDetachEvent.SDX_DETACH_CLUSTER_SUCCESS_EVENT;
import static com.sequenceiq.datalake.flow.detach.SdxDetachEvent.SDX_DETACH_EVENT;
import static com.sequenceiq.datalake.flow.detach.SdxDetachEvent.SDX_DETACH_EXTERNAL_DB_FAILED_EVENT;
import static com.sequenceiq.datalake.flow.detach.SdxDetachEvent.SDX_DETACH_EXTERNAL_DB_SUCCESS_EVENT;
import static com.sequenceiq.datalake.flow.detach.SdxDetachEvent.SDX_DETACH_FAILED_EVENT;
import static com.sequenceiq.datalake.flow.detach.SdxDetachEvent.SDX_DETACH_FAILED_HANDLED_EVENT;
import static com.sequenceiq.datalake.flow.detach.SdxDetachEvent.SDX_DETACH_STACK_FAILED_EVENT;
import static com.sequenceiq.datalake.flow.detach.SdxDetachEvent.SDX_DETACH_STACK_SUCCESS_EVENT;
import static com.sequenceiq.datalake.flow.detach.SdxDetachEvent.SDX_DETACH_STACK_SUCCESS_WITH_EXTERNAL_DB_EVENT;
import static com.sequenceiq.datalake.flow.detach.SdxDetachState.FINAL_STATE;
import static com.sequenceiq.datalake.flow.detach.SdxDetachState.INIT_STATE;
import static com.sequenceiq.datalake.flow.detach.SdxDetachState.SDX_ATTACH_NEW_CLUSTER_FAILED_STATE;
import static com.sequenceiq.datalake.flow.detach.SdxDetachState.SDX_ATTACH_NEW_CLUSTER_STATE;
import static com.sequenceiq.datalake.flow.detach.SdxDetachState.SDX_DETACH_CLUSTER_STATE;
import static com.sequenceiq.datalake.flow.detach.SdxDetachState.SDX_DETACH_EXTERNAL_DB_FAILED_STATE;
import static com.sequenceiq.datalake.flow.detach.SdxDetachState.SDX_DETACH_EXTERNAL_DB_STATE;
import static com.sequenceiq.datalake.flow.detach.SdxDetachState.SDX_DETACH_FAILED_STATE;
import static com.sequenceiq.datalake.flow.detach.SdxDetachState.SDX_DETACH_STACK_FAILED_STATE;
import static com.sequenceiq.datalake.flow.detach.SdxDetachState.SDX_DETACH_STACK_STATE;

import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.flow.core.config.AbstractFlowConfiguration;
import com.sequenceiq.flow.core.config.RetryableFlowConfiguration;

@Component
public class SdxDetachFlowConfig extends AbstractFlowConfiguration<SdxDetachState, SdxDetachEvent> implements RetryableFlowConfiguration<SdxDetachEvent> {

    private static final List<Transition<SdxDetachState, SdxDetachEvent>> TRANSITIONS = new Transition.Builder<SdxDetachState, SdxDetachEvent>()
            .defaultFailureEvent(SDX_DETACH_FAILED_EVENT)

            // Main flow.
            .from(INIT_STATE).to(SDX_DETACH_CLUSTER_STATE)
            .event(SDX_DETACH_EVENT).noFailureEvent()

            .from(SDX_DETACH_CLUSTER_STATE).to(SDX_DETACH_STACK_STATE)
            .event(SDX_DETACH_CLUSTER_SUCCESS_EVENT).defaultFailureEvent()

            .from(SDX_DETACH_STACK_STATE).to(SDX_DETACH_EXTERNAL_DB_STATE)
            .event(SDX_DETACH_STACK_SUCCESS_WITH_EXTERNAL_DB_EVENT)
            .failureState(SDX_DETACH_STACK_FAILED_STATE)
            .failureEvent(SDX_DETACH_STACK_FAILED_EVENT)

            .from(SDX_DETACH_STACK_STATE).to(SDX_ATTACH_NEW_CLUSTER_STATE)
            .event(SDX_DETACH_STACK_SUCCESS_EVENT)
            .failureState(SDX_DETACH_STACK_FAILED_STATE)
            .failureEvent(SDX_DETACH_STACK_FAILED_EVENT)

            .from(SDX_DETACH_STACK_FAILED_STATE).to(SDX_DETACH_FAILED_STATE)
            .event(SDX_DETACH_FAILED_EVENT).defaultFailureEvent()

            .from(SDX_DETACH_EXTERNAL_DB_STATE).to(SDX_ATTACH_NEW_CLUSTER_STATE)
            .event(SDX_DETACH_EXTERNAL_DB_SUCCESS_EVENT)
            .failureState(SDX_DETACH_EXTERNAL_DB_FAILED_STATE)
            .failureEvent(SDX_DETACH_EXTERNAL_DB_FAILED_EVENT)

            .from(SDX_DETACH_EXTERNAL_DB_FAILED_STATE).to(SDX_DETACH_FAILED_STATE)
            .event(SDX_DETACH_FAILED_EVENT).defaultFailureEvent()

            .from(SDX_ATTACH_NEW_CLUSTER_STATE).to(FINAL_STATE)
            .event(SDX_ATTACH_NEW_CLUSTER_SUCCESS_EVENT)
            .failureState(SDX_ATTACH_NEW_CLUSTER_FAILED_STATE)
            .failureEvent(SDX_ATTACH_NEW_CLUSTER_FAILED_EVENT)
            .from(SDX_ATTACH_NEW_CLUSTER_FAILED_STATE).to(SDX_DETACH_FAILED_STATE)
            .event(SDX_DETACH_FAILED_EVENT).defaultFailureEvent()

            // General failure configuration.
            .from(SDX_DETACH_FAILED_STATE).to(FINAL_STATE)
            .event(SDX_DETACH_FAILED_HANDLED_EVENT).noFailureEvent()

            .build();

    private static final FlowEdgeConfig<SdxDetachState, SdxDetachEvent> EDGE_CONFIG =
            new FlowEdgeConfig<>(INIT_STATE, FINAL_STATE, SDX_DETACH_FAILED_STATE, SDX_DETACH_FAILED_HANDLED_EVENT);

    public SdxDetachFlowConfig() {
        super(SdxDetachState.class, SdxDetachEvent.class);
    }

    @Override
    public SdxDetachEvent[] getEvents() {
        return SdxDetachEvent.values();
    }

    @Override
    public SdxDetachEvent[] getInitEvents() {
        return new SdxDetachEvent[] {
                SDX_DETACH_EVENT
        };
    }

    @Override
    public String getDisplayName() {
        return "Sdx detach";
    }

    @Override
    protected List<Transition<SdxDetachState, SdxDetachEvent>> getTransitions() {
        return TRANSITIONS;
    }

    @Override
    protected FlowEdgeConfig<SdxDetachState, SdxDetachEvent> getEdgeConfig() {
        return EDGE_CONFIG;
    }

    @Override
    public SdxDetachEvent getRetryableEvent() {
        return SDX_DETACH_FAILED_HANDLED_EVENT;
    }
}
