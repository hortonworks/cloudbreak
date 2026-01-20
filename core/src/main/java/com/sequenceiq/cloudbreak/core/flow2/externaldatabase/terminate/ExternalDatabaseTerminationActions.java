package com.sequenceiq.cloudbreak.core.flow2.externaldatabase.terminate;

import java.util.Map;

import jakarta.inject.Inject;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.action.Action;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.externaldatabase.ExternalDatabaseContext;
import com.sequenceiq.cloudbreak.core.flow2.externaldatabase.StackUpdaterService;
import com.sequenceiq.cloudbreak.core.flow2.externaldatabase.terminate.action.AbstractExternalDatabaseTerminationAction;
import com.sequenceiq.cloudbreak.core.flow2.externaldatabase.terminate.config.ExternalDatabaseTerminationEvent;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.externaldatabase.TerminateExternalDatabaseFailed;
import com.sequenceiq.cloudbreak.reactor.api.event.externaldatabase.TerminateExternalDatabaseRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.externaldatabase.TerminateExternalDatabaseResult;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.TerminationEvent;
import com.sequenceiq.cloudbreak.service.metrics.MetricType;
import com.sequenceiq.cloudbreak.view.StackView;

@Configuration
public class ExternalDatabaseTerminationActions {

    @Inject
    private StackUpdaterService stackUpdaterService;

    @Bean(name = "WAIT_FOR_EXTERNAL_DATABASE_TERMINATION_STATE")
    public Action<?, ?> externalDatabaseTermination() {
        return new AbstractExternalDatabaseTerminationAction<>(TerminationEvent.class) {
            @Override
            protected void doExecute(ExternalDatabaseContext context, TerminationEvent payload, Map<Object, Object> variables) {
                StackView stack = context.getStack();
                TerminateExternalDatabaseRequest request = new TerminateExternalDatabaseRequest(stack.getId(), "TerminateExternalDatabaseRequest",
                        stack.getName(), stack.getResourceCrn(), payload.getTerminationType().isForced());
                sendEvent(context, request);
            }
        };
    }

    @Bean(name = "EXTERNAL_DATABASE_TERMINATION_FINISHED_STATE")
    public Action<?, ?> externalDatabaseTerminationFinishedAction() {
        return new AbstractExternalDatabaseTerminationAction<>(TerminateExternalDatabaseResult.class) {
            @Override
            protected void doExecute(ExternalDatabaseContext context, TerminateExternalDatabaseResult payload, Map<Object, Object> variables) {
                getMetricService().incrementMetricCounter(MetricType.EXTERNAL_DATABASE_TERMINATION_SUCCESSFUL, context.getStack());
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(ExternalDatabaseContext context) {
                return new StackEvent(ExternalDatabaseTerminationEvent.EXTERNAL_DATABASE_TERMINATION_FINISHED_EVENT.event(), context.getStackId());
            }
        };
    }

    @Bean(name = "EXTERNAL_DATABASE_TERMINATION_FAILED_STATE")
    public Action<?, ?> externalDatabaseTerminationFailureAction() {
        return new AbstractExternalDatabaseTerminationAction<>(TerminateExternalDatabaseFailed.class) {

            @Override
            protected void doExecute(ExternalDatabaseContext context, TerminateExternalDatabaseFailed payload, Map<Object, Object> variables) {
                stackUpdaterService.updateStatus(context.getStackId(), DetailedStackStatus.DELETE_FAILED,
                        ResourceEvent.CLUSTER_EXTERNAL_DATABASE_DELETION_FAILED, payload.getException().getMessage());
                getMetricService().incrementMetricCounter(MetricType.EXTERNAL_DATABASE_TERMINATION_FAILED, context.getStack());
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(ExternalDatabaseContext context) {
                return new StackEvent(ExternalDatabaseTerminationEvent.EXTERNAL_DATABASE_TERMINATION_FAILURE_HANDLED_EVENT.event(), context.getStackId());
            }
        };
    }

}
