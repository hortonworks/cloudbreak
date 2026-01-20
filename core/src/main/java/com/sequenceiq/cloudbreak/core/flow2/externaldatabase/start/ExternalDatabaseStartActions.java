package com.sequenceiq.cloudbreak.core.flow2.externaldatabase.start;

import java.util.Map;

import jakarta.inject.Inject;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.action.Action;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.externaldatabase.ExternalDatabaseContext;
import com.sequenceiq.cloudbreak.core.flow2.externaldatabase.StackUpdaterService;
import com.sequenceiq.cloudbreak.core.flow2.externaldatabase.start.action.AbstractExternalDatabaseStartAction;
import com.sequenceiq.cloudbreak.core.flow2.externaldatabase.start.config.ExternalDatabaseStartEvent;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.externaldatabase.StartExternalDatabaseFailed;
import com.sequenceiq.cloudbreak.reactor.api.event.externaldatabase.StartExternalDatabaseRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.externaldatabase.StartExternalDatabaseResult;
import com.sequenceiq.cloudbreak.service.metrics.MetricType;
import com.sequenceiq.cloudbreak.view.StackView;

@Configuration
public class ExternalDatabaseStartActions {

    @Inject
    private StackUpdaterService stackUpdaterService;

    @Bean(name = "EXTERNAL_DATABASE_STARTING_STATE")
    public Action<?, ?> externalDatabaseStartRequestAction() {
        return new AbstractExternalDatabaseStartAction<>(StackEvent.class) {
            @Override
            protected void doExecute(ExternalDatabaseContext context, StackEvent payload, Map<Object, Object> variables) {
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(ExternalDatabaseContext context) {
                StackView stack = context.getStack();
                return new StartExternalDatabaseRequest(stack.getId(), "StartExternalDatabaseRequest", stack.getName(), stack.getResourceCrn());
            }
        };
    }

    @Bean(name = "EXTERNAL_DATABASE_START_FINISHED_STATE")
    public Action<?, ?> externalDatabaseStartFinishedAction() {
        return new AbstractExternalDatabaseStartAction<>(StartExternalDatabaseResult.class) {
            @Override
            protected void doExecute(ExternalDatabaseContext context, StartExternalDatabaseResult payload, Map<Object, Object> variables) {
                getMetricService().incrementMetricCounter(MetricType.EXTERNAL_DATABASE_START_SUCCESSFUL, context.getStack());
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(ExternalDatabaseContext context) {
                return new StackEvent(ExternalDatabaseStartEvent.EXTERNAL_DATABASE_START_FINALIZED_EVENT.event(), context.getStackId());
            }
        };
    }

    @Bean(name = "EXTERNAL_DATABASE_START_FAILED_STATE")
    public Action<?, ?> externalDatabaseStartFailureAction() {
        return new AbstractExternalDatabaseStartAction<>(StartExternalDatabaseFailed.class) {

            @Override
            protected void doExecute(ExternalDatabaseContext context, StartExternalDatabaseFailed payload, Map<Object, Object> variables) {
                stackUpdaterService.updateStatus(context.getStackId(), DetailedStackStatus.EXTERNAL_DATABASE_START_FAILED,
                        ResourceEvent.CLUSTER_EXTERNAL_DATABASE_START_FAILED, payload.getException().getMessage());
                getMetricService().incrementMetricCounter(MetricType.EXTERNAL_DATABASE_START_FAILED, context.getStack());
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(ExternalDatabaseContext context) {
                return new StackEvent(ExternalDatabaseStartEvent.EXTERNAL_DATABASE_START_FAILURE_HANDLED_EVENT.event(), context.getStackId());
            }
        };
    }

}
