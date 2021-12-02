package com.sequenceiq.cloudbreak.core.flow2.externaldatabase.provision.config;

import static com.sequenceiq.cloudbreak.core.flow2.externaldatabase.provision.config.ExternalDatabaseCreationEvent.EXTERNAL_DATABASE_CREATION_FAILED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.externaldatabase.provision.config.ExternalDatabaseCreationEvent.EXTERNAL_DATABASE_CREATION_FAILURE_HANDLED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.externaldatabase.provision.config.ExternalDatabaseCreationEvent.EXTERNAL_DATABASE_CREATION_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.externaldatabase.provision.config.ExternalDatabaseCreationEvent.EXTERNAL_DATABASE_WAIT_SUCCESS_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.externaldatabase.provision.config.ExternalDatabaseCreationEvent.START_EXTERNAL_DATABASE_CREATION_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.externaldatabase.provision.config.ExternalDatabaseCreationState.EXTERNAL_DATABASE_CREATION_FAILED_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.externaldatabase.provision.config.ExternalDatabaseCreationState.EXTERNAL_DATABASE_CREATION_FINISHED_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.externaldatabase.provision.config.ExternalDatabaseCreationState.FINAL_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.externaldatabase.provision.config.ExternalDatabaseCreationState.INIT_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.externaldatabase.provision.config.ExternalDatabaseCreationState.WAIT_FOR_EXTERNAL_DATABASE_STATE;

import java.util.List;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.core.flow2.StackStatusFinalizer;
import com.sequenceiq.flow.core.config.AbstractFlowConfiguration;
import com.sequenceiq.flow.core.config.AbstractFlowConfiguration.Transition.Builder;
import com.sequenceiq.flow.core.config.FlowFinalizerCallback;
import com.sequenceiq.flow.core.config.RetryableFlowConfiguration;

@Component
public class ExternalDatabaseCreationFlowConfig extends AbstractFlowConfiguration<ExternalDatabaseCreationState, ExternalDatabaseCreationEvent>
        implements RetryableFlowConfiguration<ExternalDatabaseCreationEvent> {

    private static final List<Transition<ExternalDatabaseCreationState, ExternalDatabaseCreationEvent>> TRANSITIONS =
            new Builder<ExternalDatabaseCreationState, ExternalDatabaseCreationEvent>()
            .defaultFailureEvent(EXTERNAL_DATABASE_CREATION_FAILED_EVENT)
            .from(INIT_STATE)
            .to(WAIT_FOR_EXTERNAL_DATABASE_STATE)
            .event(START_EXTERNAL_DATABASE_CREATION_EVENT).defaultFailureEvent()
            .from(WAIT_FOR_EXTERNAL_DATABASE_STATE)
            .to(EXTERNAL_DATABASE_CREATION_FINISHED_STATE)
            .event(EXTERNAL_DATABASE_WAIT_SUCCESS_EVENT).defaultFailureEvent()
            .from(EXTERNAL_DATABASE_CREATION_FINISHED_STATE)
            .to(FINAL_STATE)
            .event(EXTERNAL_DATABASE_CREATION_FINISHED_EVENT).defaultFailureEvent()
            .build();

    private static final FlowEdgeConfig<ExternalDatabaseCreationState, ExternalDatabaseCreationEvent> EDGE_CONFIG =
            new FlowEdgeConfig<>(INIT_STATE, FINAL_STATE, EXTERNAL_DATABASE_CREATION_FAILED_STATE, EXTERNAL_DATABASE_CREATION_FAILURE_HANDLED_EVENT);

    @Inject
    private StackStatusFinalizer stackStatusFinalizer;

    public ExternalDatabaseCreationFlowConfig() {
        super(ExternalDatabaseCreationState.class, ExternalDatabaseCreationEvent.class);
    }

    @Override
    protected List<Transition<ExternalDatabaseCreationState, ExternalDatabaseCreationEvent>> getTransitions() {
        return TRANSITIONS;
    }

    @Override
    protected FlowEdgeConfig<ExternalDatabaseCreationState, ExternalDatabaseCreationEvent> getEdgeConfig() {
        return EDGE_CONFIG;
    }

    @Override
    public ExternalDatabaseCreationEvent[] getEvents() {
        return ExternalDatabaseCreationEvent.values();
    }

    @Override
    public ExternalDatabaseCreationEvent[] getInitEvents() {
        return new ExternalDatabaseCreationEvent[] {
                START_EXTERNAL_DATABASE_CREATION_EVENT
        };
    }

    @Override
    public String getDisplayName() {
        return "Create external database for stack";
    }

    @Override
    public ExternalDatabaseCreationEvent getRetryableEvent() {
        return EXTERNAL_DATABASE_CREATION_FAILURE_HANDLED_EVENT;
    }

    @Override
    public FlowFinalizerCallback getFinalizerCallBack() {
        return stackStatusFinalizer;
    }
}
