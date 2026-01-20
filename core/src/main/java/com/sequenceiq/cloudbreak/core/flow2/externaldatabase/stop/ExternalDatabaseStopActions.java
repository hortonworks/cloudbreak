package com.sequenceiq.cloudbreak.core.flow2.externaldatabase.stop;

import java.util.Map;

import jakarta.inject.Inject;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.action.Action;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.externaldatabase.ExternalDatabaseContext;
import com.sequenceiq.cloudbreak.core.flow2.externaldatabase.StackUpdaterService;
import com.sequenceiq.cloudbreak.core.flow2.externaldatabase.stop.action.AbstractExternalDatabaseStopAction;
import com.sequenceiq.cloudbreak.core.flow2.externaldatabase.stop.config.ExternalDatabaseStopEvent;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.externaldatabase.StopExternalDatabaseFailed;
import com.sequenceiq.cloudbreak.reactor.api.event.externaldatabase.StopExternalDatabaseRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.externaldatabase.StopExternalDatabaseResult;
import com.sequenceiq.cloudbreak.service.metrics.MetricType;
import com.sequenceiq.cloudbreak.view.StackView;

@Configuration
public class ExternalDatabaseStopActions {

    @Inject
    private StackUpdaterService stackUpdaterService;

    @Bean(name = "EXTERNAL_DATABASE_STOPPING_STATE")
    public Action<?, ?> externalDatabaseStopRequestAction() {
        return new AbstractExternalDatabaseStopAction<>(StackEvent.class) {
            @Override
            protected void doExecute(ExternalDatabaseContext context, StackEvent payload, Map<Object, Object> variables) {
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(ExternalDatabaseContext context) {
                StackView stack = context.getStack();
                return new StopExternalDatabaseRequest(stack.getId(), "StopExternalDatabaseRequest", stack.getName(), stack.getResourceCrn());
            }
        };
    }

    @Bean(name = "EXTERNAL_DATABASE_STOP_FINISHED_STATE")
    public Action<?, ?> externalDatabaseStopFinishedAction() {
        return new AbstractExternalDatabaseStopAction<>(StopExternalDatabaseResult.class) {
            @Override
            protected void doExecute(ExternalDatabaseContext context, StopExternalDatabaseResult payload, Map<Object, Object> variables) {
                getMetricService().incrementMetricCounter(MetricType.EXTERNAL_DATABASE_STOP_SUCCESSFUL, context.getStack());
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(ExternalDatabaseContext context) {
                return new StackEvent(ExternalDatabaseStopEvent.EXTERNAL_DATABASE_STOP_FINALIZED_EVENT.event(), context.getStackId());
            }
        };
    }

    @Bean(name = "EXTERNAL_DATABASE_STOP_FAILED_STATE")
    public Action<?, ?> externalDatabaseStopFailureAction() {
        return new AbstractExternalDatabaseStopAction<>(StopExternalDatabaseFailed.class) {

            @Override
            protected void doExecute(ExternalDatabaseContext context, StopExternalDatabaseFailed payload, Map<Object, Object> variables) {
                stackUpdaterService.updateStatus(context.getStackId(), DetailedStackStatus.EXTERNAL_DATABASE_STOP_FAILED,
                        ResourceEvent.CLUSTER_EXTERNAL_DATABASE_STOP_FAILED, payload.getException().getMessage());
                getMetricService().incrementMetricCounter(MetricType.EXTERNAL_DATABASE_STOP_FAILED, context.getStack());
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(ExternalDatabaseContext context) {
                return new StackEvent(ExternalDatabaseStopEvent.EXTERNAL_DATABASE_STOP_FAILURE_HANDLED_EVENT.event(), context.getStackId());
            }
        };
    }

}
