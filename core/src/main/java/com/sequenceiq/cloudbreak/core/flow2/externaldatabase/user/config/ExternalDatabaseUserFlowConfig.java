package com.sequenceiq.cloudbreak.core.flow2.externaldatabase.user.config;

import static com.sequenceiq.cloudbreak.core.flow2.externaldatabase.user.config.ExternalDatabaseUserEvent.EXTERNAL_DATABASE_USER_OPERATION_FAILED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.externaldatabase.user.config.ExternalDatabaseUserEvent.EXTERNAL_DATABASE_USER_OPERATION_FAILURE_HANDLED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.externaldatabase.user.config.ExternalDatabaseUserEvent.EXTERNAL_DATABASE_USER_OPERATION_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.externaldatabase.user.config.ExternalDatabaseUserEvent.EXTERNAL_DATABASE_USER_OPERATION_SUCCESS_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.externaldatabase.user.config.ExternalDatabaseUserEvent.START_EXTERNAL_DATABASE_USER_OPERATION_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.externaldatabase.user.config.ExternalDatabaseUserState.EXECUTE_EXTERNAL_DATABASE_USER_OPERATION_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.externaldatabase.user.config.ExternalDatabaseUserState.EXTERNAL_DATABASE_USER_OPERATION_FAILED_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.externaldatabase.user.config.ExternalDatabaseUserState.EXTERNAL_DATABASE_USER_OPERATION_FINISHED_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.externaldatabase.user.config.ExternalDatabaseUserState.FINAL_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.externaldatabase.user.config.ExternalDatabaseUserState.INIT_STATE;

import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.core.flow2.StackStatusFinalizerAbstractFlowConfig;
import com.sequenceiq.flow.core.config.RetryableFlowConfiguration;

@Component
public class ExternalDatabaseUserFlowConfig extends StackStatusFinalizerAbstractFlowConfig<ExternalDatabaseUserState, ExternalDatabaseUserEvent>
        implements RetryableFlowConfiguration<ExternalDatabaseUserEvent> {

    private static final List<Transition<ExternalDatabaseUserState, ExternalDatabaseUserEvent>> TRANSITIONS =
            new Transition.Builder<ExternalDatabaseUserState, ExternalDatabaseUserEvent>()
                    .defaultFailureEvent(EXTERNAL_DATABASE_USER_OPERATION_FAILED_EVENT)
                    .from(INIT_STATE)
                    .to(EXECUTE_EXTERNAL_DATABASE_USER_OPERATION_STATE)
                    .event(START_EXTERNAL_DATABASE_USER_OPERATION_EVENT).defaultFailureEvent()
                    .from(EXECUTE_EXTERNAL_DATABASE_USER_OPERATION_STATE)
                    .to(EXTERNAL_DATABASE_USER_OPERATION_FINISHED_STATE)
                    .event(EXTERNAL_DATABASE_USER_OPERATION_SUCCESS_EVENT).defaultFailureEvent()
                    .from(EXTERNAL_DATABASE_USER_OPERATION_FINISHED_STATE)
                    .to(FINAL_STATE)
                    .event(EXTERNAL_DATABASE_USER_OPERATION_FINISHED_EVENT).defaultFailureEvent()
                    .build();

    private static final FlowEdgeConfig<ExternalDatabaseUserState, ExternalDatabaseUserEvent> EDGE_CONFIG =
            new FlowEdgeConfig<>(INIT_STATE, FINAL_STATE, EXTERNAL_DATABASE_USER_OPERATION_FAILED_STATE, EXTERNAL_DATABASE_USER_OPERATION_FAILURE_HANDLED_EVENT);

    public ExternalDatabaseUserFlowConfig() {
        super(ExternalDatabaseUserState.class, ExternalDatabaseUserEvent.class);
    }

    @Override
    protected List<Transition<ExternalDatabaseUserState, ExternalDatabaseUserEvent>> getTransitions() {
        return TRANSITIONS;
    }

    @Override
    public FlowEdgeConfig<ExternalDatabaseUserState, ExternalDatabaseUserEvent> getEdgeConfig() {
        return EDGE_CONFIG;
    }

    @Override
    public ExternalDatabaseUserEvent[] getEvents() {
        return ExternalDatabaseUserEvent.values();
    }

    @Override
    public ExternalDatabaseUserEvent[] getInitEvents() {
        return new ExternalDatabaseUserEvent[] {
                START_EXTERNAL_DATABASE_USER_OPERATION_EVENT
        };
    }

    @Override
    public String getDisplayName() {
        return "Execute user operation (create or delete) for external database of the stack";
    }

    @Override
    public ExternalDatabaseUserEvent getRetryableEvent() {
        return EXTERNAL_DATABASE_USER_OPERATION_FAILURE_HANDLED_EVENT;
    }
}
