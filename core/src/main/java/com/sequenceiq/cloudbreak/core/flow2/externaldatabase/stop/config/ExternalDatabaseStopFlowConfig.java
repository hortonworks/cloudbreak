package com.sequenceiq.cloudbreak.core.flow2.externaldatabase.stop.config;

import static com.sequenceiq.cloudbreak.core.flow2.externaldatabase.stop.config.ExternalDatabaseStopEvent.EXTERNAL_DATABASE_COMMENCE_STOP_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.externaldatabase.stop.config.ExternalDatabaseStopEvent.EXTERNAL_DATABASE_STOPPED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.externaldatabase.stop.config.ExternalDatabaseStopEvent.EXTERNAL_DATABASE_STOP_FAILED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.externaldatabase.stop.config.ExternalDatabaseStopEvent.EXTERNAL_DATABASE_STOP_FAILURE_HANDLED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.externaldatabase.stop.config.ExternalDatabaseStopEvent.EXTERNAL_DATABASE_STOP_FINALIZED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.externaldatabase.stop.config.ExternalDatabaseStopState.EXTERNAL_DATABASE_STOPPING_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.externaldatabase.stop.config.ExternalDatabaseStopState.EXTERNAL_DATABASE_STOP_FAILED_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.externaldatabase.stop.config.ExternalDatabaseStopState.EXTERNAL_DATABASE_STOP_FINISHED_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.externaldatabase.stop.config.ExternalDatabaseStopState.FINAL_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.externaldatabase.stop.config.ExternalDatabaseStopState.INIT_STATE;

import java.util.List;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.core.flow2.StackStatusFinalizer;
import com.sequenceiq.flow.core.config.AbstractFlowConfiguration;
import com.sequenceiq.flow.core.config.AbstractFlowConfiguration.Transition.Builder;
import com.sequenceiq.flow.core.config.FlowFinalizerCallback;
import com.sequenceiq.flow.core.config.RetryableFlowConfiguration;

@Component
public class ExternalDatabaseStopFlowConfig extends AbstractFlowConfiguration<ExternalDatabaseStopState, ExternalDatabaseStopEvent>
        implements RetryableFlowConfiguration<ExternalDatabaseStopEvent> {

    private static final List<Transition<ExternalDatabaseStopState, ExternalDatabaseStopEvent>> TRANSITIONS =
            new Builder<ExternalDatabaseStopState, ExternalDatabaseStopEvent>()
            .defaultFailureEvent(EXTERNAL_DATABASE_STOP_FAILED_EVENT)
            .from(INIT_STATE)
            .to(EXTERNAL_DATABASE_STOPPING_STATE)
            .event(EXTERNAL_DATABASE_COMMENCE_STOP_EVENT).defaultFailureEvent()
            .from(EXTERNAL_DATABASE_STOPPING_STATE)
            .to(EXTERNAL_DATABASE_STOP_FINISHED_STATE)
            .event(EXTERNAL_DATABASE_STOPPED_EVENT).defaultFailureEvent()
            .from(EXTERNAL_DATABASE_STOP_FINISHED_STATE)
            .to(FINAL_STATE)
            .event(EXTERNAL_DATABASE_STOP_FINALIZED_EVENT).defaultFailureEvent()
            .build();

    private static final FlowEdgeConfig<ExternalDatabaseStopState, ExternalDatabaseStopEvent> EDGE_CONFIG =
            new FlowEdgeConfig<>(INIT_STATE, FINAL_STATE, EXTERNAL_DATABASE_STOP_FAILED_STATE, EXTERNAL_DATABASE_STOP_FAILURE_HANDLED_EVENT);

    @Inject
    private StackStatusFinalizer stackStatusFinalizer;

    public ExternalDatabaseStopFlowConfig() {
        super(ExternalDatabaseStopState.class, ExternalDatabaseStopEvent.class);
    }

    @Override
    protected List<Transition<ExternalDatabaseStopState, ExternalDatabaseStopEvent>> getTransitions() {
        return TRANSITIONS;
    }

    @Override
    protected FlowEdgeConfig<ExternalDatabaseStopState, ExternalDatabaseStopEvent> getEdgeConfig() {
        return EDGE_CONFIG;
    }

    @Override
    public ExternalDatabaseStopEvent[] getEvents() {
        return ExternalDatabaseStopEvent.values();
    }

    @Override
    public ExternalDatabaseStopEvent[] getInitEvents() {
        return new ExternalDatabaseStopEvent[] {
                EXTERNAL_DATABASE_COMMENCE_STOP_EVENT
        };
    }

    @Override
    public String getDisplayName() {
        return "Stop external database of stack";
    }

    @Override
    public ExternalDatabaseStopEvent getRetryableEvent() {
        return EXTERNAL_DATABASE_STOP_FAILURE_HANDLED_EVENT;
    }

    @Override
    public FlowFinalizerCallback getFinalizerCallBack() {
        return stackStatusFinalizer;
    }
}
