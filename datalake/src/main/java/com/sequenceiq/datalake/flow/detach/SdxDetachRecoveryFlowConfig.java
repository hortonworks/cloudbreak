package com.sequenceiq.datalake.flow.detach;

import static com.sequenceiq.datalake.flow.detach.SdxDetachRecoveryEvent.SDX_DETACH_RECOVERY_EVENT;
import static com.sequenceiq.datalake.flow.detach.SdxDetachRecoveryEvent.SDX_DETACH_RECOVERY_FAILED_EVENT;
import static com.sequenceiq.datalake.flow.detach.SdxDetachRecoveryEvent.SDX_DETACH_RECOVERY_FAILURE_HANDLED_EVENT;
import static com.sequenceiq.datalake.flow.detach.SdxDetachRecoveryEvent.SDX_DETACH_RECOVERY_SUCCESS_EVENT;
import static com.sequenceiq.datalake.flow.detach.SdxDetachRecoveryState.FINAL_STATE;
import static com.sequenceiq.datalake.flow.detach.SdxDetachRecoveryState.INIT_STATE;
import static com.sequenceiq.datalake.flow.detach.SdxDetachRecoveryState.SDX_DETACH_RECOVERY_FAILED_STATE;
import static com.sequenceiq.datalake.flow.detach.SdxDetachRecoveryState.SDX_DETACH_RECOVERY_STATE;

import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.datalake.flow.RetryableDatalakeFlowConfiguration;
import com.sequenceiq.flow.core.config.AbstractFlowConfiguration;
import com.sequenceiq.flow.core.config.AbstractFlowConfiguration.Transition.Builder;

@Component
public class SdxDetachRecoveryFlowConfig extends AbstractFlowConfiguration<SdxDetachRecoveryState, SdxDetachRecoveryEvent>
        implements RetryableDatalakeFlowConfiguration<SdxDetachRecoveryEvent> {

    private static final List<Transition<SdxDetachRecoveryState, SdxDetachRecoveryEvent>> TRANSITIONS =
            new Builder<SdxDetachRecoveryState, SdxDetachRecoveryEvent>()
                    .defaultFailureEvent(SDX_DETACH_RECOVERY_FAILED_EVENT)

                    .from(INIT_STATE).to(SDX_DETACH_RECOVERY_STATE)
                    .event(SDX_DETACH_RECOVERY_EVENT).noFailureEvent()

                    .from(SDX_DETACH_RECOVERY_STATE).to(FINAL_STATE)
                    .event(SDX_DETACH_RECOVERY_SUCCESS_EVENT).defaultFailureEvent()

                    .from(SDX_DETACH_RECOVERY_FAILED_STATE).to(FINAL_STATE)
                    .event(SDX_DETACH_RECOVERY_FAILURE_HANDLED_EVENT).noFailureEvent()

                    .build();

    private static final FlowEdgeConfig<SdxDetachRecoveryState, SdxDetachRecoveryEvent> EDGE_CONFIG =
            new FlowEdgeConfig<>(INIT_STATE, FINAL_STATE, SDX_DETACH_RECOVERY_FAILED_STATE, SDX_DETACH_RECOVERY_FAILURE_HANDLED_EVENT);

    public SdxDetachRecoveryFlowConfig() {
        super(SdxDetachRecoveryState.class, SdxDetachRecoveryEvent.class);
    }

    @Override
    public SdxDetachRecoveryEvent[] getEvents() {
        return SdxDetachRecoveryEvent.values();
    }

    @Override
    public SdxDetachRecoveryEvent[] getInitEvents() {
        return new SdxDetachRecoveryEvent[] {
                SDX_DETACH_RECOVERY_EVENT
        };
    }

    @Override
    public String getDisplayName() {
        return "Sdx detach recovery";
    }

    @Override
    protected List<Transition<SdxDetachRecoveryState, SdxDetachRecoveryEvent>> getTransitions() {
        return TRANSITIONS;
    }

    @Override
    public FlowEdgeConfig<SdxDetachRecoveryState, SdxDetachRecoveryEvent> getEdgeConfig() {
        return EDGE_CONFIG;
    }

    @Override
    public SdxDetachRecoveryEvent getRetryableEvent() {
        return SDX_DETACH_RECOVERY_FAILURE_HANDLED_EVENT;
    }
}
