package com.sequenceiq.datalake.flow.enableselinux.config;

import static com.sequenceiq.datalake.flow.enableselinux.DatalakeEnableSeLinuxState.ENABLE_SELINUX_DATALAKE_FAILED_STATE;
import static com.sequenceiq.datalake.flow.enableselinux.DatalakeEnableSeLinuxState.ENABLE_SELINUX_DATALAKE_FINISHED_STATE;
import static com.sequenceiq.datalake.flow.enableselinux.DatalakeEnableSeLinuxState.ENABLE_SELINUX_DATALAKE_STATE;
import static com.sequenceiq.datalake.flow.enableselinux.DatalakeEnableSeLinuxState.ENABLE_SELINUX_DATALAKE_VALIDATION_STATE;
import static com.sequenceiq.datalake.flow.enableselinux.DatalakeEnableSeLinuxState.FINAL_STATE;
import static com.sequenceiq.datalake.flow.enableselinux.DatalakeEnableSeLinuxState.INIT_STATE;
import static com.sequenceiq.datalake.flow.enableselinux.event.DatalakeEnableSeLinuxStateSelectors.ENABLE_SELINUX_DATALAKE_EVENT;
import static com.sequenceiq.datalake.flow.enableselinux.event.DatalakeEnableSeLinuxStateSelectors.ENABLE_SELINUX_DATALAKE_VALIDATION_EVENT;
import static com.sequenceiq.datalake.flow.enableselinux.event.DatalakeEnableSeLinuxStateSelectors.FAILED_ENABLE_SELINUX_DATALAKE_EVENT;
import static com.sequenceiq.datalake.flow.enableselinux.event.DatalakeEnableSeLinuxStateSelectors.FINALIZE_ENABLE_SELINUX_DATALAKE_EVENT;
import static com.sequenceiq.datalake.flow.enableselinux.event.DatalakeEnableSeLinuxStateSelectors.FINISH_ENABLE_SELINUX_DATALAKE_EVENT;
import static com.sequenceiq.datalake.flow.enableselinux.event.DatalakeEnableSeLinuxStateSelectors.HANDLED_FAILED_ENABLE_SELINUX_DATALAKE_EVENT;

import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.datalake.flow.enableselinux.DatalakeEnableSeLinuxState;
import com.sequenceiq.datalake.flow.enableselinux.event.DatalakeEnableSeLinuxStateSelectors;
import com.sequenceiq.flow.core.config.AbstractFlowConfiguration;
import com.sequenceiq.flow.core.config.RetryableFlowConfiguration;

@Component
public class DatalakeEnableSeLinuxFlowConfig extends AbstractFlowConfiguration<DatalakeEnableSeLinuxState, DatalakeEnableSeLinuxStateSelectors>
        implements RetryableFlowConfiguration<DatalakeEnableSeLinuxStateSelectors> {

    private static final List<Transition<DatalakeEnableSeLinuxState, DatalakeEnableSeLinuxStateSelectors>> TRANSITIONS =
            new Transition.Builder<DatalakeEnableSeLinuxState, DatalakeEnableSeLinuxStateSelectors>()
            .defaultFailureEvent(FAILED_ENABLE_SELINUX_DATALAKE_EVENT)

            .from(DatalakeEnableSeLinuxState.INIT_STATE)
            .to(ENABLE_SELINUX_DATALAKE_VALIDATION_STATE)
            .event(ENABLE_SELINUX_DATALAKE_VALIDATION_EVENT)
            .defaultFailureEvent()

            .from(ENABLE_SELINUX_DATALAKE_VALIDATION_STATE)
            .to(ENABLE_SELINUX_DATALAKE_STATE)
            .event(ENABLE_SELINUX_DATALAKE_EVENT)
            .defaultFailureEvent()

            .from(ENABLE_SELINUX_DATALAKE_STATE)
            .to(ENABLE_SELINUX_DATALAKE_FINISHED_STATE)
            .event(FINISH_ENABLE_SELINUX_DATALAKE_EVENT)
            .defaultFailureEvent()

            .from(ENABLE_SELINUX_DATALAKE_FINISHED_STATE)
            .to(FINAL_STATE)
            .event(FINALIZE_ENABLE_SELINUX_DATALAKE_EVENT)
            .defaultFailureEvent()

            .build();

    protected DatalakeEnableSeLinuxFlowConfig() {
        super(DatalakeEnableSeLinuxState.class, DatalakeEnableSeLinuxStateSelectors.class);
    }

    @Override
    protected List<Transition<DatalakeEnableSeLinuxState, DatalakeEnableSeLinuxStateSelectors>> getTransitions() {
        return TRANSITIONS;
    }

    @Override
    public FlowEdgeConfig<DatalakeEnableSeLinuxState, DatalakeEnableSeLinuxStateSelectors> getEdgeConfig() {
        return new FlowEdgeConfig<>(
                INIT_STATE,
                FINAL_STATE,
                ENABLE_SELINUX_DATALAKE_FAILED_STATE,
                HANDLED_FAILED_ENABLE_SELINUX_DATALAKE_EVENT);
    }

    @Override
    public DatalakeEnableSeLinuxStateSelectors[] getEvents() {
        return DatalakeEnableSeLinuxStateSelectors.values();
    }

    @Override
    public DatalakeEnableSeLinuxStateSelectors[] getInitEvents() {
        return new DatalakeEnableSeLinuxStateSelectors[] {ENABLE_SELINUX_DATALAKE_VALIDATION_EVENT};
    }

    @Override
    public String getDisplayName() {
        return "Enable SeLinux Datalake";
    }

    @Override
    public DatalakeEnableSeLinuxStateSelectors getRetryableEvent() {
        return HANDLED_FAILED_ENABLE_SELINUX_DATALAKE_EVENT;
    }
}
