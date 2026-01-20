package com.sequenceiq.cloudbreak.core.flow2.validate.cloud;

import java.util.Map;
import java.util.Optional;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.externaldatabase.StackUpdaterService;
import com.sequenceiq.cloudbreak.core.flow2.stack.AbstractStackFailureAction;
import com.sequenceiq.cloudbreak.core.flow2.stack.StackFailureContext;
import com.sequenceiq.cloudbreak.core.flow2.stack.provision.action.AbstractStackCreationAction;
import com.sequenceiq.cloudbreak.core.flow2.stack.start.StackCreationContext;
import com.sequenceiq.cloudbreak.core.flow2.validate.cloud.config.CloudConfigValidationEvent;
import com.sequenceiq.cloudbreak.core.flow2.validate.cloud.config.CloudConfigValidationState;
import com.sequenceiq.cloudbreak.core.flow2.validate.cloud.event.ValidateCloudConfigRequest;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.view.StackView;
import com.sequenceiq.flow.core.FlowParameters;

@Configuration
public class CloudConfigValidationActions {

    private static final Logger LOGGER = LoggerFactory.getLogger(CloudConfigValidationActions.class);

    @Inject
    private StackUpdaterService stackUpdaterService;

    @Inject
    private StackDtoService stackDtoService;

    @Bean(name = "VALIDATE_CLOUD_CONFIG_STATE")
    public Action<?, ?> cloudConfigValidationAction() {
        return new AbstractStackCreationAction<>(StackEvent.class) {
            @Override
            protected void doExecute(StackCreationContext context, StackEvent payload, Map<Object, Object> variables) throws Exception {
                ValidateCloudConfigRequest request = new ValidateCloudConfigRequest(payload.getResourceId());
                sendEvent(context, request.selector(), request);
            }

            @Override
            protected Object getFailurePayload(StackEvent payload, Optional<StackCreationContext> flowContext, Exception ex) {
                return new StackFailureEvent(CloudConfigValidationEvent.VALIDATE_CLOUD_CONFIG_FAILED_EVENT.selector(), payload.getResourceId(), ex);
            }
        };
    }

    @Bean(name = "VALIDATE_CLOUD_CONFIG_FAILED_STATE")
    public Action<?, ?> cloudConfigValidationFailureAction() {
        return new AbstractStackFailureAction<CloudConfigValidationState, CloudConfigValidationEvent>() {

            @Override
            protected StackFailureContext createFlowContext(FlowParameters flowParameters,
                    StateContext<CloudConfigValidationState, CloudConfigValidationEvent> stateContext, StackFailureEvent payload) {
                StackView stack = stackDtoService.getStackViewById(payload.getResourceId());
                MDCBuilder.buildMdcContext(stack);
                return new StackFailureContext(flowParameters, stack, stack.getId());
            }

            @Override
            protected void doExecute(StackFailureContext context, StackFailureEvent payload, Map<Object, Object> variables) {
                String statusReason = payload.getException().getMessage();
                stackUpdaterService.updateStatusAndSendEventWithArgs(context.getStackId(), DetailedStackStatus.PROVISION_FAILED,
                        ResourceEvent.CLOUD_CONFIG_VALIDATION_FAILED, statusReason, statusReason);
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(StackFailureContext context) {
                return new StackEvent(CloudConfigValidationEvent.VALIDATE_CLOUD_CONFIG_FAILURE_HANDLED_EVENT.selector(), context.getStackId());
            }
        };
    }
}
