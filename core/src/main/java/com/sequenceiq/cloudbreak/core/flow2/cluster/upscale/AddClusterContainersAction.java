package com.sequenceiq.cloudbreak.core.flow2.cluster.upscale;

import java.util.Map;

import javax.inject.Inject;

import org.springframework.statemachine.StateContext;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow.context.ClusterScalingContext;
import com.sequenceiq.cloudbreak.core.flow2.AbstractAction;
import com.sequenceiq.cloudbreak.core.flow2.MessageFactory;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.AddClusterContainersRequest;
import com.sequenceiq.cloudbreak.service.stack.StackService;

@Component("AddClusterContainersAction")
public class AddClusterContainersAction extends AbstractAction<ClusterUpscaleState, ClusterUpscaleEvent, AddClusterContainersContext, ClusterScalingContext> {

    @Inject
    private StackService stackService;

    protected AddClusterContainersAction() {
        super(ClusterScalingContext.class);
    }

    @Override
    protected AddClusterContainersContext createFlowContext(StateContext<ClusterUpscaleState, ClusterUpscaleEvent> stateContext,
            ClusterScalingContext payload) {
        String flowId = (String) stateContext.getMessageHeader(MessageFactory.HEADERS.FLOW_ID.name());
        Stack stack = stackService.getById(payload.getStackId());
        MDCBuilder.buildMdcContext(stack);
        return new AddClusterContainersContext(flowId, stack, payload.getHostGroupName(), payload.getScalingAdjustment());
    }

    @Override
    protected void doExecute(AddClusterContainersContext context, ClusterScalingContext payload, Map<Object, Object> variables) throws Exception {
        sendEvent(context);
    }

    @Override
    protected Selectable createRequest(AddClusterContainersContext context) {
        return new AddClusterContainersRequest(context.getStack().getId(), context.getHostGroupName(), context.getScalingAdjustment());
    }

    @Override
    protected Object getFailurePayload(AddClusterContainersContext flowContext, Exception ex) {
        return new UpscaleClusterFailedPayload(flowContext.getStack().getId(), flowContext.getHostGroupName(), ex);
    }
}
