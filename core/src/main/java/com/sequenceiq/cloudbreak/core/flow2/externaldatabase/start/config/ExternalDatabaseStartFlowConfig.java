package com.sequenceiq.cloudbreak.core.flow2.externaldatabase.start.config;

import static com.sequenceiq.cloudbreak.core.flow2.externaldatabase.start.config.ExternalDatabaseStartEvent.EXTERNAL_DATABASE_COMMENCE_START_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.externaldatabase.start.config.ExternalDatabaseStartEvent.EXTERNAL_DATABASE_STARTED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.externaldatabase.start.config.ExternalDatabaseStartEvent.EXTERNAL_DATABASE_START_FAILED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.externaldatabase.start.config.ExternalDatabaseStartEvent.EXTERNAL_DATABASE_START_FAILURE_HANDLED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.externaldatabase.start.config.ExternalDatabaseStartEvent.EXTERNAL_DATABASE_START_FINALIZED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.externaldatabase.start.config.ExternalDatabaseStartState.EXTERNAL_DATABASE_STARTING_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.externaldatabase.start.config.ExternalDatabaseStartState.EXTERNAL_DATABASE_START_FAILED_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.externaldatabase.start.config.ExternalDatabaseStartState.EXTERNAL_DATABASE_START_FINISHED_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.externaldatabase.start.config.ExternalDatabaseStartState.FINAL_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.externaldatabase.start.config.ExternalDatabaseStartState.INIT_STATE;

import java.util.List;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.core.flow2.StackStatusFinalizer;
import com.sequenceiq.flow.core.config.AbstractFlowConfiguration;
import com.sequenceiq.flow.core.config.AbstractFlowConfiguration.Transition.Builder;
import com.sequenceiq.flow.core.config.FlowFinalizerCallback;
import com.sequenceiq.flow.core.config.RetryableFlowConfiguration;

@Component
public class ExternalDatabaseStartFlowConfig extends AbstractFlowConfiguration<ExternalDatabaseStartState, ExternalDatabaseStartEvent>
        implements RetryableFlowConfiguration<ExternalDatabaseStartEvent> {

    private static final List<Transition<ExternalDatabaseStartState, ExternalDatabaseStartEvent>> TRANSITIONS =
            new Builder<ExternalDatabaseStartState, ExternalDatabaseStartEvent>()
            .defaultFailureEvent(EXTERNAL_DATABASE_START_FAILED_EVENT)
            .from(INIT_STATE)
            .to(EXTERNAL_DATABASE_STARTING_STATE)
            .event(EXTERNAL_DATABASE_COMMENCE_START_EVENT).defaultFailureEvent()
            .from(EXTERNAL_DATABASE_STARTING_STATE)
            .to(EXTERNAL_DATABASE_START_FINISHED_STATE)
            .event(EXTERNAL_DATABASE_STARTED_EVENT).defaultFailureEvent()
            .from(EXTERNAL_DATABASE_START_FINISHED_STATE)
            .to(FINAL_STATE)
            .event(EXTERNAL_DATABASE_START_FINALIZED_EVENT).defaultFailureEvent()
            .build();

    private static final FlowEdgeConfig<ExternalDatabaseStartState, ExternalDatabaseStartEvent> EDGE_CONFIG =
            new FlowEdgeConfig<>(INIT_STATE, FINAL_STATE, EXTERNAL_DATABASE_START_FAILED_STATE, EXTERNAL_DATABASE_START_FAILURE_HANDLED_EVENT);

    @Inject
    private StackStatusFinalizer stackStatusFinalizer;

    public ExternalDatabaseStartFlowConfig() {
        super(ExternalDatabaseStartState.class, ExternalDatabaseStartEvent.class);
    }

    @Override
    protected List<Transition<ExternalDatabaseStartState, ExternalDatabaseStartEvent>> getTransitions() {
        return TRANSITIONS;
    }

    @Override
    protected FlowEdgeConfig<ExternalDatabaseStartState, ExternalDatabaseStartEvent> getEdgeConfig() {
        return EDGE_CONFIG;
    }

    @Override
    public ExternalDatabaseStartEvent[] getEvents() {
        return ExternalDatabaseStartEvent.values();
    }

    @Override
    public ExternalDatabaseStartEvent[] getInitEvents() {
        return new ExternalDatabaseStartEvent[] {
                EXTERNAL_DATABASE_COMMENCE_START_EVENT
        };
    }

    @Override
    public String getDisplayName() {
        return "Start external database of stack";
    }

    @Override
    public ExternalDatabaseStartEvent getRetryableEvent() {
        return EXTERNAL_DATABASE_START_FAILURE_HANDLED_EVENT;
    }

    @Override
    public FlowFinalizerCallback getFinalizerCallBack() {
        return stackStatusFinalizer;
    }
}
