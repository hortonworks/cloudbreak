package com.sequenceiq.datalake.flow.datalake.restartservices;

import static com.sequenceiq.datalake.flow.datalake.restartservices.DatalakeRestartServicesFlowEvent.DATALAKE_RESTART_SERVICES_FAILED_EVENT;
import static com.sequenceiq.datalake.flow.datalake.restartservices.DatalakeRestartServicesFlowEvent.DATALAKE_RESTART_SERVICES_FAILED_HANDLED_EVENT;
import static com.sequenceiq.datalake.flow.datalake.restartservices.DatalakeRestartServicesFlowEvent.DATALAKE_RESTART_SERVICES_FINALIZED_EVENT;
import static com.sequenceiq.datalake.flow.datalake.restartservices.DatalakeRestartServicesFlowEvent.DATALAKE_RESTART_SERVICES_FINISHED_EVENT;
import static com.sequenceiq.datalake.flow.datalake.restartservices.DatalakeRestartServicesFlowEvent.DATALAKE_RESTART_SERVICES_IN_PROGRESS_EVENT;
import static com.sequenceiq.datalake.flow.datalake.restartservices.DatalakeRestartServicesFlowEvent.DATALAKE_RESTART_SERVICES_START_EVENT;
import static com.sequenceiq.datalake.flow.datalake.restartservices.DatalakeRestartServicesFlowState.DATALAKE_RESTART_SERVICES_FAILED_STATE;
import static com.sequenceiq.datalake.flow.datalake.restartservices.DatalakeRestartServicesFlowState.DATALAKE_RESTART_SERVICES_FINISHED_STATE;
import static com.sequenceiq.datalake.flow.datalake.restartservices.DatalakeRestartServicesFlowState.DATALAKE_RESTART_SERVICES_IN_PROGRESS_STATE;
import static com.sequenceiq.datalake.flow.datalake.restartservices.DatalakeRestartServicesFlowState.DATALAKE_RESTART_SERVICES_START_STATE;
import static com.sequenceiq.datalake.flow.datalake.restartservices.DatalakeRestartServicesFlowState.FINAL_STATE;
import static com.sequenceiq.datalake.flow.datalake.restartservices.DatalakeRestartServicesFlowState.INIT_STATE;

import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.datalake.flow.RetryableDatalakeFlowConfiguration;
import com.sequenceiq.flow.core.config.AbstractFlowConfiguration;
import com.sequenceiq.flow.core.config.AbstractFlowConfiguration.Transition.Builder;

@Component
public class DatalakeRestartServicesFlowConfig extends AbstractFlowConfiguration<DatalakeRestartServicesFlowState, DatalakeRestartServicesFlowEvent>
        implements RetryableDatalakeFlowConfiguration<DatalakeRestartServicesFlowEvent> {

    private static final List<Transition<DatalakeRestartServicesFlowState, DatalakeRestartServicesFlowEvent>> TRANSITIONS
            = new Builder<DatalakeRestartServicesFlowState, DatalakeRestartServicesFlowEvent>()
            .defaultFailureEvent(DatalakeRestartServicesFlowEvent.DATALAKE_RESTART_SERVICES_FAILED_EVENT)

            .from(INIT_STATE)
            .to(DATALAKE_RESTART_SERVICES_START_STATE)
            .event(DATALAKE_RESTART_SERVICES_START_EVENT)
            .defaultFailureEvent()

            .from(DATALAKE_RESTART_SERVICES_START_STATE)
            .to(DATALAKE_RESTART_SERVICES_IN_PROGRESS_STATE)
            .event(DATALAKE_RESTART_SERVICES_IN_PROGRESS_EVENT)
            .defaultFailureEvent()

            .from(DATALAKE_RESTART_SERVICES_IN_PROGRESS_STATE)
            .to(DATALAKE_RESTART_SERVICES_FINISHED_STATE)
            .event(DATALAKE_RESTART_SERVICES_FINISHED_EVENT)
            .defaultFailureEvent()

            .from(DATALAKE_RESTART_SERVICES_FINISHED_STATE)
            .to(FINAL_STATE)
            .event(DATALAKE_RESTART_SERVICES_FINALIZED_EVENT)
            .defaultFailureEvent()
            .build();

    protected DatalakeRestartServicesFlowConfig() {
        super(DatalakeRestartServicesFlowState.class, DatalakeRestartServicesFlowEvent.class);
    }

    @Override
    protected List<Transition<DatalakeRestartServicesFlowState, DatalakeRestartServicesFlowEvent>> getTransitions() {
        return TRANSITIONS;
    }

    @Override
    public FlowEdgeConfig<DatalakeRestartServicesFlowState, DatalakeRestartServicesFlowEvent> getEdgeConfig() {
        return new FlowEdgeConfig<>(INIT_STATE, FINAL_STATE, DATALAKE_RESTART_SERVICES_FAILED_STATE, DATALAKE_RESTART_SERVICES_FAILED_HANDLED_EVENT);
    }

    @Override
    public DatalakeRestartServicesFlowEvent[] getEvents() {
        return DatalakeRestartServicesFlowEvent.values();
    }

    @Override
    public DatalakeRestartServicesFlowEvent[] getInitEvents() {
        return new DatalakeRestartServicesFlowEvent[]{DATALAKE_RESTART_SERVICES_START_EVENT};
    }

    @Override
    public String getDisplayName() {
        return "Restart Data Lake Services";
    }

    @Override
    public DatalakeRestartServicesFlowEvent getRetryableEvent() {
        return DATALAKE_RESTART_SERVICES_FAILED_EVENT;
    }
}
