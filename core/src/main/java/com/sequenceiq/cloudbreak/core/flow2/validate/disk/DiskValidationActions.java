package com.sequenceiq.cloudbreak.core.flow2.validate.disk;

import java.util.List;
import java.util.Map;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.event.DiskValidationTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.externaldatabase.StackUpdaterService;
import com.sequenceiq.cloudbreak.core.flow2.stack.AbstractStackFailureAction;
import com.sequenceiq.cloudbreak.core.flow2.stack.StackContext;
import com.sequenceiq.cloudbreak.core.flow2.stack.StackFailureContext;
import com.sequenceiq.cloudbreak.core.flow2.validate.disk.config.DiskValidationEvent;
import com.sequenceiq.cloudbreak.core.flow2.validate.disk.config.DiskValidationState;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.ValidateDiskRequest;
import com.sequenceiq.cloudbreak.service.stack.DiskValidationService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.view.StackView;
import com.sequenceiq.flow.core.Flow;
import com.sequenceiq.flow.core.FlowParameters;

@Configuration
public class DiskValidationActions {

    private static final Logger LOGGER = LoggerFactory.getLogger(DiskValidationActions.class);

    @Inject
    private StackUpdaterService stackUpdaterService;

    @Inject
    private StackDtoService stackDtoService;

    @Inject
    private DiskValidationService diskValidationService;

    @Bean(name = "DISK_VALIDATION_STATE")
    public AbstractDiskValidationAction<?> diskValidationAction() {
        return new AbstractDiskValidationAction<>(DiskValidationTriggerEvent.class) {
            @Override
            protected void doExecute(StackContext context, DiskValidationTriggerEvent payload, Map<Object, Object> variables) throws Exception {
                StackView stack = context.getStack().getStack();
                List<CloudResource> disksToCheck = diskValidationService.getVolumesForValidation(stack, payload.getRepairableGroupsWithHostNames());
                ValidateDiskRequest<Selectable> request =
                        new ValidateDiskRequest<>(context.getCloudContext(), context.getCloudCredential(), context.getCloudStack(), disksToCheck);
                sendEvent(context, request);
            }
        };
    }

    @Bean(name = "DISK_VALIDATION_FAILED_STATE")
    public Action<?, ?> diskValidationFailureAction() {
        return new AbstractStackFailureAction<DiskValidationState, DiskValidationEvent>() {

            @Override
            protected StackFailureContext createFlowContext(FlowParameters flowParameters,
                    StateContext<DiskValidationState, DiskValidationEvent> stateContext, StackFailureEvent payload) {
                Flow flow = getFlow(flowParameters.getFlowId());
                StackView stack = stackDtoService.getStackViewById(payload.getResourceId());
                MDCBuilder.buildMdcContext(stack);
                flow.setFlowFailed(payload.getException());
                return new StackFailureContext(flowParameters, stack, stack.getId());
            }

            @Override
            protected void doExecute(StackFailureContext context, StackFailureEvent payload, Map<Object, Object> variables) {
                String statusReason = payload.getException().getMessage();
                stackUpdaterService.updateStatusAndSendEventWithArgs(context.getStackId(), DetailedStackStatus.REPAIR_FAILED,
                        ResourceEvent.DISK_VALIDATION_FAILED, statusReason, statusReason);
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(StackFailureContext context) {
                return new StackEvent(DiskValidationEvent.DISK_VALIDATION_FAILURE_HANDLED_EVENT.selector(), context.getStackId());
            }
        };
    }
}
