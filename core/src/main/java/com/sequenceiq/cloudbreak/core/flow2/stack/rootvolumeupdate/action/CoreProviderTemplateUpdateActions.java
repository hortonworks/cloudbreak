package com.sequenceiq.cloudbreak.core.flow2.stack.rootvolumeupdate.action;

import static com.sequenceiq.cloudbreak.core.flow2.stack.rootvolumeupdate.CoreProviderTemplateUpdateFlowEvent.CORE_PROVIDER_TEMPLATE_UPDATE_FAIL_HANDLED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.rootvolumeupdate.CoreProviderTemplateUpdateFlowEvent.CORE_PROVIDER_TEMPLATE_UPDATE_FINALIZED_EVENT;

import java.util.Map;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.core.flow2.stack.StackContext;
import com.sequenceiq.cloudbreak.core.flow2.stack.rootvolumeupdate.CoreProviderTemplateUpdateFlowEvent;
import com.sequenceiq.cloudbreak.core.flow2.stack.rootvolumeupdate.CoreProviderTemplateUpdateState;
import com.sequenceiq.cloudbreak.core.flow2.stack.rootvolumeupdate.ProviderTemplateUpdateHandlerRequest;
import com.sequenceiq.cloudbreak.core.flow2.stack.rootvolumeupdate.event.CoreProviderTemplateUpdateEvent;
import com.sequenceiq.cloudbreak.core.flow2.stack.rootvolumeupdate.event.CoreProviderTemplateUpdateFailureEvent;
import com.sequenceiq.cloudbreak.dto.StackDtoDelegate;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.service.StackUpdater;
import com.sequenceiq.flow.core.Flow;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.flow.event.EventSelectorUtil;

@Configuration
public class CoreProviderTemplateUpdateActions {

    private static final Logger LOGGER = LoggerFactory.getLogger(CoreProviderTemplateUpdateActions.class);

    @Inject
    private StackUpdater stackUpdater;

    @Bean(name = "CORE_PROVIDER_TEMPLATE_UPDATE_STATE")
    public Action<?, ?> launchTemplateUpdateAction() {
        return new CoreAbstractProviderTemplateUpdateAction<>(CoreProviderTemplateUpdateEvent.class) {
            @Override
            protected void doExecute(StackContext context, CoreProviderTemplateUpdateEvent payload, Map<Object, Object> variables) {
                StackDtoDelegate stack = context.getStack();
                LOGGER.info("Updating launch template {}", payload);
                stackUpdater.updateStackStatus(stack.getId(), DetailedStackStatus.PROVIDER_TEMPLATE_UPDATE_IN_PROGRESS, "Starting to update launch template.");
                String selector = EventSelectorUtil.selector(ProviderTemplateUpdateHandlerRequest.class);
                sendEvent(context, selector, new ProviderTemplateUpdateHandlerRequest(selector, stack.getId(), context.getCloudContext(),
                        context.getCloudCredential(), context.getCloudStack()));
            }
        };
    }

    @Bean(name = "CORE_PROVIDER_TEMPLATE_UPDATE_FINISHED_STATE")
    public Action<?, ?> launchTemplateUpdateFinishedAction() {
        return new CoreAbstractProviderTemplateUpdateAction<>(CoreProviderTemplateUpdateEvent.class) {
            @Override
            protected void doExecute(StackContext context, CoreProviderTemplateUpdateEvent payload, Map<Object, Object> variables) {
                StackDtoDelegate stack = context.getStack();
                stackUpdater.updateStackStatus(stack.getId(), DetailedStackStatus.PROVIDER_TEMPLATE_UPDATE_COMPLETE, "Updating Launch Template complete.");
                sendEvent(context, CORE_PROVIDER_TEMPLATE_UPDATE_FINALIZED_EVENT.selector(), new StackEvent(stack.getId()));
            }
        };
    }

    @Bean(name = "CORE_PROVIDER_TEMPLATE_UPDATE_FAIL_STATE")
    public Action<?, ?> failureAction() {
        return new CoreAbstractProviderTemplateUpdateAction<>(CoreProviderTemplateUpdateFailureEvent.class) {
            @Override
            protected StackContext createFlowContext(FlowParameters flowParameters,
                    StateContext<CoreProviderTemplateUpdateState, CoreProviderTemplateUpdateFlowEvent> stateContext,
                    CoreProviderTemplateUpdateFailureEvent payload) {
                Flow flow = getFlow(flowParameters.getFlowId());
                flow.setFlowFailed(payload.getException());
                return super.createFlowContext(flowParameters, stateContext, payload);
            }

            @Override
            protected void doExecute(StackContext context, CoreProviderTemplateUpdateFailureEvent payload, Map<Object, Object> variables) {
                LOGGER.error("Updating launch template failed with payload: " + payload);
                StackDtoDelegate stack = context.getStack();
                String errorReason = getErrorReason(payload.getException());
                stackUpdater.updateStackStatus(stack.getId(), DetailedStackStatus.PROVIDER_TEMPLATE_UPDATE_FAILED, errorReason);
                sendEvent(context, CORE_PROVIDER_TEMPLATE_UPDATE_FAIL_HANDLED_EVENT.event(), payload);
            }

            private String getErrorReason(Exception payloadException) {
                return payloadException == null || payloadException.getMessage() == null ? "Unknown error" : payloadException.getMessage();
            }
        };
    }
}
