package com.sequenceiq.cloudbreak.core.flow2.externaldatabase.user;

import static com.sequenceiq.flow.event.EventSelectorUtil.selector;

import java.util.Map;

import jakarta.inject.Inject;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.action.Action;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.externaldatabase.ExternalDatabaseContext;
import com.sequenceiq.cloudbreak.core.flow2.externaldatabase.StackUpdaterService;
import com.sequenceiq.cloudbreak.core.flow2.externaldatabase.user.action.AbstractExternalDatabaseUserAction;
import com.sequenceiq.cloudbreak.core.flow2.externaldatabase.user.config.ExternalDatabaseUserEvent;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.externaldatabase.UserOperationExternalDatabaseFailed;
import com.sequenceiq.cloudbreak.reactor.api.event.externaldatabase.UserOperationExternalDatabaseRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.externaldatabase.UserOperationExternalDatabaseResult;
import com.sequenceiq.cloudbreak.view.StackView;

@Configuration
public class ExternalDatabaseUserActions  {

    @Inject
    private StackUpdaterService stackUpdaterService;

    @Bean(name = "EXECUTE_EXTERNAL_DATABASE_USER_OPERATION_STATE")
    public Action<?, ?> externalDatabaseUserOperation() {
        return new AbstractExternalDatabaseUserAction<>(ExternalDatabaseUserFlowStartEvent.class) {
            @Override
            protected void doExecute(ExternalDatabaseContext context, ExternalDatabaseUserFlowStartEvent payload, Map<Object, Object> variables) {
                StackView stack = context.getStack();
                UserOperationExternalDatabaseRequest request = new UserOperationExternalDatabaseRequest(stack.getId(),
                        selector(UserOperationExternalDatabaseRequest.class), stack.getName(), stack.getResourceCrn(),
                        payload.getOperation(), payload.getDatabaseType(), payload.getDatabaseUser());
                stackUpdaterService.updateStatus(stack.getId(), DetailedStackStatus.EXT_DB_USER_OP_STARTED,
                        ResourceEvent.EXT_DB_USER_OP_STARTED, "Database user operation started.");
                sendEvent(context, request);
            }
        };
    }

    @Bean(name = "EXTERNAL_DATABASE_USER_OPERATION_FINISHED_STATE")
    public Action<?, ?> externalDatabaseUserOperationFinishedAction() {
        return new AbstractExternalDatabaseUserAction<>(UserOperationExternalDatabaseResult.class) {
            @Override
            protected void doExecute(ExternalDatabaseContext context, UserOperationExternalDatabaseResult payload, Map<Object, Object> variables) {
                stackUpdaterService.updateStatus(payload.getResourceId(), DetailedStackStatus.EXT_DB_USER_OP_FINISHED,
                        ResourceEvent.EXT_DB_USER_OP_FINISHED, "Database user operation finished.");
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(ExternalDatabaseContext context) {
                return new StackEvent(ExternalDatabaseUserEvent.EXTERNAL_DATABASE_USER_OPERATION_FINISHED_EVENT.event(), context.getStackId());
            }
        };
    }

    @Bean(name = "EXTERNAL_DATABASE_USER_OPERATION_FAILED_STATE")
    public Action<?, ?> externalDatabaseUserOperationFailedAction() {
        return new AbstractExternalDatabaseUserAction<>(UserOperationExternalDatabaseFailed.class) {

            @Override
            protected void doExecute(ExternalDatabaseContext context, UserOperationExternalDatabaseFailed payload, Map<Object, Object> variables) {
                stackUpdaterService.updateStatus(payload.getResourceId(), DetailedStackStatus.EXT_DB_USER_OP_FAILED,
                        ResourceEvent.EXT_DB_USER_OP_FAILED, "Database user operation failed.");
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(ExternalDatabaseContext context) {
                return new StackEvent(ExternalDatabaseUserEvent.EXTERNAL_DATABASE_USER_OPERATION_FAILURE_HANDLED_EVENT.event(), context.getStackId());
            }
        };
    }

}
