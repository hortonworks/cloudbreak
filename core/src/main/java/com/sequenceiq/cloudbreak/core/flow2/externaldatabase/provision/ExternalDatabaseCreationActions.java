package com.sequenceiq.cloudbreak.core.flow2.externaldatabase.provision;

import java.util.Map;

import jakarta.inject.Inject;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.action.Action;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.externaldatabase.ExternalDatabaseContext;
import com.sequenceiq.cloudbreak.core.flow2.externaldatabase.StackUpdaterService;
import com.sequenceiq.cloudbreak.core.flow2.externaldatabase.provision.action.AbstractExternalDatabaseCreationAction;
import com.sequenceiq.cloudbreak.core.flow2.externaldatabase.provision.config.ExternalDatabaseCreationEvent;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.externaldatabase.CreateExternalDatabaseFailed;
import com.sequenceiq.cloudbreak.reactor.api.event.externaldatabase.CreateExternalDatabaseRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.externaldatabase.CreateExternalDatabaseResult;
import com.sequenceiq.cloudbreak.service.metrics.MetricType;
import com.sequenceiq.cloudbreak.view.StackView;

@Configuration
public class ExternalDatabaseCreationActions {

    @Inject
    private StackUpdaterService stackUpdaterService;

    @Bean(name = "WAIT_FOR_EXTERNAL_DATABASE_STATE")
    public Action<?, ?> externalDatabaseCreation() {
        return new AbstractExternalDatabaseCreationAction<>(StackEvent.class) {
            @Override
            protected void doExecute(ExternalDatabaseContext context, StackEvent payload, Map<Object, Object> variables) {
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(ExternalDatabaseContext context) {
                StackView stack = context.getStack();
                return new CreateExternalDatabaseRequest(stack.getId(), "CreateExternalDatabaseRequest", stack.getName(), stack.getResourceCrn());
            }
        };
    }

    @Bean(name = "EXTERNAL_DATABASE_CREATION_FINISHED_STATE")
    public Action<?, ?> externalDatabaseCreationFinishedAction() {
        return new AbstractExternalDatabaseCreationAction<>(CreateExternalDatabaseResult.class) {
            @Override
            protected void doExecute(ExternalDatabaseContext context, CreateExternalDatabaseResult payload, Map<Object, Object> variables) {
                getMetricService().incrementMetricCounter(MetricType.EXTERNAL_DATABASE_CREATION_SUCCESSFUL, context.getStack());
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(ExternalDatabaseContext context) {
                return new StackEvent(ExternalDatabaseCreationEvent.EXTERNAL_DATABASE_CREATION_FINISHED_EVENT.event(), context.getStackId());
            }
        };
    }

    @Bean(name = "EXTERNAL_DATABASE_CREATION_FAILED_STATE")
    public Action<?, ?> externalDatabaseCreationFailureAction() {
        return new AbstractExternalDatabaseCreationAction<>(CreateExternalDatabaseFailed.class) {

            @Override
            protected void doExecute(ExternalDatabaseContext context, CreateExternalDatabaseFailed payload, Map<Object, Object> variables) {
                stackUpdaterService.updateStatus(context.getStackId(), DetailedStackStatus.EXTERNAL_DATABASE_CREATION_FAILED,
                        ResourceEvent.CLUSTER_EXTERNAL_DATABASE_CREATION_FAILED, payload.getException().getMessage());
                getMetricService().incrementMetricCounter(MetricType.EXTERNAL_DATABASE_CREATION_FAILED, context.getStack());
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(ExternalDatabaseContext context) {
                return new StackEvent(ExternalDatabaseCreationEvent.EXTERNAL_DATABASE_CREATION_FAILURE_HANDLED_EVENT.event(), context.getStackId());
            }
        };
    }

}
