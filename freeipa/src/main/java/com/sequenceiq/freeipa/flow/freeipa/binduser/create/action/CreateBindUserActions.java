package com.sequenceiq.freeipa.flow.freeipa.binduser.create.action;

import static com.sequenceiq.cloudbreak.event.ResourceEvent.FREEIPA_BIND_USER_CREATE_FAILED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.FREEIPA_BIND_USER_CREATE_FINISHED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.FREEIPA_BIND_USER_CREATE_STARTED;

import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.action.Action;

import com.sequenceiq.flow.core.CommonContext;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SuccessDetails;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.events.EventSenderService;
import com.sequenceiq.freeipa.flow.freeipa.binduser.create.event.CreateBindUserEvent;
import com.sequenceiq.freeipa.flow.freeipa.binduser.create.event.CreateBindUserFailureEvent;
import com.sequenceiq.freeipa.flow.freeipa.binduser.create.event.CreateBindUserFlowEvent;
import com.sequenceiq.freeipa.flow.freeipa.binduser.create.event.CreateKerberosBindUserEvent;
import com.sequenceiq.freeipa.flow.freeipa.binduser.create.event.CreateLdapBindUserEvent;
import com.sequenceiq.freeipa.service.operation.OperationService;
import com.sequenceiq.freeipa.service.stack.StackService;

@Configuration
public class CreateBindUserActions {

    private static final Logger LOGGER = LoggerFactory.getLogger(CreateBindUserActions.class);

    @Inject
    private EventSenderService eventSenderService;

    @Inject
    private StackService stackService;

    @Bean("CREATE_KERBEROS_BIND_USER_STATE")
    public Action<?, ?> createKerberosBindUserAction() {
        return new AbstractBindUserCreateAction<>(CreateBindUserEvent.class) {
            @Override
            protected void doExecute(CommonContext context, CreateBindUserEvent payload, Map<Object, Object> variables) {
                LOGGER.info("Sending request to create Kerberos bind user for {}", payload.getSuffix());
                sendNotification(payload, context, FREEIPA_BIND_USER_CREATE_STARTED, List.of(payload.getSuffix()));
                sendEvent(context, new CreateKerberosBindUserEvent(payload));
            }
        };
    }

    @Bean("CREATE_LDAP_BIND_USER_STATE")
    public Action<?, ?> createLdapBindUserAction() {
        return new AbstractBindUserCreateAction<>(CreateBindUserEvent.class) {
            @Override
            protected void doExecute(CommonContext context, CreateBindUserEvent payload, Map<Object, Object> variables) {
                LOGGER.info("Sending request to create LDAP bind user for {}", payload.getSuffix());
                sendEvent(context, new CreateLdapBindUserEvent(payload));
            }
        };
    }

    @Bean("CREATE_BIND_USER_FINISHED_STATE")
    public Action<?, ?> createBindUserFinishedAction() {
        return new AbstractBindUserCreateAction<>(CreateBindUserEvent.class) {

            @Inject
            private OperationService operationService;

            @Override
            protected void doExecute(CommonContext context, CreateBindUserEvent payload, Map<Object, Object> variables) throws Exception {
                LOGGER.info("Bind user creation successfully finished with payload: {}", payload);
                sendNotification(payload, context, FREEIPA_BIND_USER_CREATE_FINISHED, List.of(payload.getSuffix()));
                SuccessDetails successDetails = new SuccessDetails(payload.getEnvironmentCrn());
                successDetails.getAdditionalDetails().put("suffix", List.of(payload.getSuffix()));
                operationService.completeOperation(payload.getAccountId(), payload.getOperationId(), Set.of(successDetails), Set.of());
                LOGGER.debug("Finalizing user creation finished");
                sendEvent(context, CreateBindUserFlowEvent.CREATE_BIND_USER_FINISHED_EVENT.event(), payload);
            }
        };
    }

    @Bean("CREATE_BIND_USER_FAILED_STATE")
    public Action<?, ?> createBindUserFailureAction() {
        return new AbstractBindUserCreateAction<>(CreateBindUserFailureEvent.class) {
            @Inject
            private OperationService operationService;

            @Override
            protected void doExecute(CommonContext context, CreateBindUserFailureEvent payload, Map<Object, Object> variables) throws Exception {
                LOGGER.error("Bind user creation failed with payload: {}", payload, payload.getException());
                String errorReason = payload.getException() == null ? payload.getFailureMessage() : payload.getException().getMessage();
                sendNotification(payload, context, FREEIPA_BIND_USER_CREATE_FAILED, List.of(payload.getSuffix(), errorReason));
                operationService.failOperation(payload.getAccountId(), payload.getOperationId(), payload.getFailureMessage());
                LOGGER.debug("Failure handling finished");
                sendEvent(context, CreateBindUserFlowEvent.CREATE_BIND_USER_FAILURE_HANDLED_EVENT.event(), payload);
            }
        };
    }

    private void sendNotification(CreateBindUserEvent payload, CommonContext context,
            com.sequenceiq.cloudbreak.event.ResourceEvent event, List<String> args) {
        Stack stack = stackService.getStackById(payload.getResourceId());
        eventSenderService.sendEventAndNotification(stack, context.getFlowTriggerUserCrn(), event, args);
    }
}
