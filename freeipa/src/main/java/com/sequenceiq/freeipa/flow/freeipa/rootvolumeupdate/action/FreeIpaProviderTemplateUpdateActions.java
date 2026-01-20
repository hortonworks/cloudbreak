package com.sequenceiq.freeipa.flow.freeipa.rootvolumeupdate.action;

import static com.sequenceiq.freeipa.flow.freeipa.rootvolumeupdate.FreeIpaProviderTemplateUpdateFlowEvent.FREEIPA_PROVIDER_TEMPLATE_UPDATE_FAIL_HANDLED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.rootvolumeupdate.FreeIpaProviderTemplateUpdateFlowEvent.FREEIPA_PROVIDER_TEMPLATE_UPDATE_FINALIZED_EVENT;

import java.util.List;
import java.util.Map;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.action.Action;

import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.freeipa.rootvolumeupdate.event.FreeIpaProviderTemplateUpdateEvent;
import com.sequenceiq.freeipa.flow.freeipa.rootvolumeupdate.event.FreeIpaProviderTemplateUpdateFailureEvent;
import com.sequenceiq.freeipa.flow.freeipa.rootvolumeupdate.event.FreeIpaProviderTemplateUpdateHandlerRequest;
import com.sequenceiq.freeipa.flow.stack.StackContext;
import com.sequenceiq.freeipa.flow.stack.StackEvent;
import com.sequenceiq.freeipa.service.operation.OperationService;
import com.sequenceiq.freeipa.service.stack.StackUpdater;

@Configuration
public class FreeIpaProviderTemplateUpdateActions {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaProviderTemplateUpdateActions.class);

    @Inject
    private StackUpdater stackUpdater;

    @Bean(name = "FREEIPA_PROVIDER_TEMPLATE_UPDATE_STATE")
    public Action<?, ?> deploymentTemplateUpdateAction() {
        return new AbstractFreeIpaProviderTemplateUpdateAction<>(FreeIpaProviderTemplateUpdateEvent.class) {
            @Override
            protected void doExecute(StackContext context, FreeIpaProviderTemplateUpdateEvent payload, Map<Object, Object> variables) {
                Stack stack = context.getStack();
                LOGGER.info("Updating launch template {}", payload);
                stackUpdater.updateStackStatus(stack, DetailedStackStatus.UPDATE_IN_PROGRESS, "Starting to update provider template.");
                String selector = EventSelectorUtil.selector(FreeIpaProviderTemplateUpdateHandlerRequest.class);
                sendEvent(context, selector, new FreeIpaProviderTemplateUpdateHandlerRequest(selector, stack.getId(), payload.getOperationId(),
                        context.getCloudContext(), context.getCloudCredential(), context.getCloudStack()));
            }
        };
    }

    @Bean(name = "FREEIPA_PROVIDER_TEMPLATE_UPDATE_FINISHED_STATE")
    public Action<?, ?> launchTemplateUpdateFinishedAction() {
        return new AbstractFreeIpaProviderTemplateUpdateAction<>(FreeIpaProviderTemplateUpdateEvent.class) {
            @Override
            protected void doExecute(StackContext context, FreeIpaProviderTemplateUpdateEvent payload, Map<Object, Object> variables) {
                Stack stack = context.getStack();
                stackUpdater.updateStackStatus(stack, DetailedStackStatus.UPDATE_COMPLETE, "Updating Launch Template complete.");
                sendEvent(context, FREEIPA_PROVIDER_TEMPLATE_UPDATE_FINALIZED_EVENT.selector(), new StackEvent(stack.getId()));
            }
        };
    }

    @Bean(name = "FREEIPA_PROVIDER_TEMPLATE_UPDATE_FAIL_STATE")
    public Action<?, ?> failureAction() {
        return new AbstractFreeIpaProviderTemplateUpdateAction<>(FreeIpaProviderTemplateUpdateFailureEvent.class) {
            @Inject
            private OperationService operationService;

            @Override
            protected void doExecute(StackContext context, FreeIpaProviderTemplateUpdateFailureEvent payload,
                    Map<Object, Object> variables) {
                Stack stack = context.getStack();
                String message = "Update Launch Template failed during " + payload.getFailedPhase();
                String errorReason = getErrorReason(payload.getException());
                stackUpdater.updateStackStatus(context.getStack(), DetailedStackStatus.UPDATE_FAILED, errorReason);
                operationService.failOperation(stack.getAccountId(), getOperationId(variables), message, List.of(), List.of());
                enableStatusChecker(stack, "Failed to update template for FreeIPA deployment.");
                sendEvent(context, FREEIPA_PROVIDER_TEMPLATE_UPDATE_FAIL_HANDLED_EVENT.event(), payload);
            }
        };
    }
}
