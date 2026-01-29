package com.sequenceiq.datalake.flow.modifyselinux.config;

import static com.sequenceiq.datalake.flow.modifyselinux.DatalakeModifySeLinuxState.FINAL_STATE;
import static com.sequenceiq.datalake.flow.modifyselinux.DatalakeModifySeLinuxState.INIT_STATE;
import static com.sequenceiq.datalake.flow.modifyselinux.DatalakeModifySeLinuxState.MODIFY_SELINUX_DATALAKE_FAILED_STATE;
import static com.sequenceiq.datalake.flow.modifyselinux.DatalakeModifySeLinuxState.MODIFY_SELINUX_DATALAKE_FINISHED_STATE;
import static com.sequenceiq.datalake.flow.modifyselinux.DatalakeModifySeLinuxState.MODIFY_SELINUX_DATALAKE_STATE;
import static com.sequenceiq.datalake.flow.modifyselinux.event.DatalakeModifySeLinuxStateSelectors.FAILED_MODIFY_SELINUX_DATALAKE_EVENT;
import static com.sequenceiq.datalake.flow.modifyselinux.event.DatalakeModifySeLinuxStateSelectors.FINALIZE_MODIFY_SELINUX_DATALAKE_EVENT;
import static com.sequenceiq.datalake.flow.modifyselinux.event.DatalakeModifySeLinuxStateSelectors.FINISH_MODIFY_SELINUX_DATALAKE_EVENT;
import static com.sequenceiq.datalake.flow.modifyselinux.event.DatalakeModifySeLinuxStateSelectors.HANDLED_FAILED_MODIFY_SELINUX_DATALAKE_EVENT;
import static com.sequenceiq.datalake.flow.modifyselinux.event.DatalakeModifySeLinuxStateSelectors.MODIFY_SELINUX_DATALAKE_EVENT;

import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.datalake.flow.RetryableDatalakeFlowConfiguration;
import com.sequenceiq.datalake.flow.modifyselinux.DatalakeModifySeLinuxState;
import com.sequenceiq.datalake.flow.modifyselinux.event.DatalakeModifySeLinuxStateSelectors;
import com.sequenceiq.flow.core.config.AbstractFlowConfiguration;

@Component
public class DatalakeModifySeLinuxFlowConfig extends AbstractFlowConfiguration<DatalakeModifySeLinuxState, DatalakeModifySeLinuxStateSelectors>
        implements RetryableDatalakeFlowConfiguration<DatalakeModifySeLinuxStateSelectors> {

    private static final List<Transition<DatalakeModifySeLinuxState, DatalakeModifySeLinuxStateSelectors>> TRANSITIONS =
            new Transition.Builder<DatalakeModifySeLinuxState, DatalakeModifySeLinuxStateSelectors>()
            .defaultFailureEvent(FAILED_MODIFY_SELINUX_DATALAKE_EVENT)

            .from(INIT_STATE)
            .to(MODIFY_SELINUX_DATALAKE_STATE)
            .event(MODIFY_SELINUX_DATALAKE_EVENT)
            .defaultFailureEvent()

            .from(MODIFY_SELINUX_DATALAKE_STATE)
            .to(MODIFY_SELINUX_DATALAKE_FINISHED_STATE)
            .event(FINISH_MODIFY_SELINUX_DATALAKE_EVENT)
            .defaultFailureEvent()

            .from(MODIFY_SELINUX_DATALAKE_FINISHED_STATE)
            .to(FINAL_STATE)
            .event(FINALIZE_MODIFY_SELINUX_DATALAKE_EVENT)
            .defaultFailureEvent()

            .build();

    protected DatalakeModifySeLinuxFlowConfig() {
        super(DatalakeModifySeLinuxState.class, DatalakeModifySeLinuxStateSelectors.class);
    }

    @Override
    protected List<Transition<DatalakeModifySeLinuxState, DatalakeModifySeLinuxStateSelectors>> getTransitions() {
        return TRANSITIONS;
    }

    @Override
    public FlowEdgeConfig<DatalakeModifySeLinuxState, DatalakeModifySeLinuxStateSelectors> getEdgeConfig() {
        return new FlowEdgeConfig<>(
                INIT_STATE,
                FINAL_STATE,
                MODIFY_SELINUX_DATALAKE_FAILED_STATE,
                HANDLED_FAILED_MODIFY_SELINUX_DATALAKE_EVENT);
    }

    @Override
    public DatalakeModifySeLinuxStateSelectors[] getEvents() {
        return DatalakeModifySeLinuxStateSelectors.values();
    }

    @Override
    public DatalakeModifySeLinuxStateSelectors[] getInitEvents() {
        return new DatalakeModifySeLinuxStateSelectors[] {MODIFY_SELINUX_DATALAKE_EVENT};
    }

    @Override
    public String getDisplayName() {
        return "Enable SeLinux Datalake";
    }

    @Override
    public DatalakeModifySeLinuxStateSelectors getRetryableEvent() {
        return HANDLED_FAILED_MODIFY_SELINUX_DATALAKE_EVENT;
    }
}
