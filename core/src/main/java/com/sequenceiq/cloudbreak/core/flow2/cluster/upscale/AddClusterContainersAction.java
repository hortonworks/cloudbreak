package com.sequenceiq.cloudbreak.core.flow2.cluster.upscale;

import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;

import org.springframework.statemachine.StateContext;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.AbstractAction;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.core.flow2.event.ClusterScaleTriggerEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.AddClusterContainersRequest;
import com.sequenceiq.cloudbreak.service.stack.StackService;

@Component("AddClusterContainersAction")
public class AddClusterContainersAction extends AbstractAction<ClusterUpscaleState, ClusterUpscaleEvent, AddClusterContainersContext, ClusterScaleTriggerEvent> {

    @Inject
    private StackService stackService;

    protected AddClusterContainersAction() {
        super(ClusterScaleTriggerEvent.class);
    }

    @Override
    protected AddClusterContainersContext createFlowContext(String flowId, StateContext<ClusterUpscaleState, ClusterUpscaleEvent> stateContext,
            ClusterScaleTriggerEvent payload) {
        Stack stack = stackService.getById(payload.getStackId());
        MDCBuilder.buildMdcContext(stack);
        return new AddClusterContainersContext(flowId, stack, payload.getHostGroupName(), payload.getAdjustment());
    }

    @Override
    protected void doExecute(AddClusterContainersContext context, ClusterScaleTriggerEvent payload, Map<Object, Object> variables) throws Exception {
        sendEvent(context);
    }

    @Override
    protected Selectable createRequest(AddClusterContainersContext context) {
        return new AddClusterContainersRequest(context.getStack().getId(), context.getHostGroupName(), context.getScalingAdjustment());
    }

    @Override
    protected Object getFailurePayload(ClusterScaleTriggerEvent payload, Optional<AddClusterContainersContext> flowContext, Exception ex) {
        UpscaleClusterFailedPayload failurePayload;
        if (flowContext.isPresent()) {
            ClusterUpscaleContext context = flowContext.get();
            failurePayload = new UpscaleClusterFailedPayload(context.getStack().getId(), context.getHostGroupName(), ex);
        } else {
            failurePayload = new UpscaleClusterFailedPayload(payload.getStackId(), null, ex);
        }
        return failurePayload;
    }
}
