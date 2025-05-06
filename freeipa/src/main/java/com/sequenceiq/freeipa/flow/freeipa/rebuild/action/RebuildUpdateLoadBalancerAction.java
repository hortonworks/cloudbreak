package com.sequenceiq.freeipa.flow.freeipa.rebuild.action;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.freeipa.loadbalancer.event.update.LoadBalancerUpdateRequest;
import com.sequenceiq.freeipa.flow.stack.StackContext;
import com.sequenceiq.freeipa.flow.stack.StackEvent;

@Component("RebuildUpdateLoadBalancerAction")
public class RebuildUpdateLoadBalancerAction extends AbstractRebuildAction<StackEvent> {

    protected RebuildUpdateLoadBalancerAction() {
        super(StackEvent.class);
    }

    @Override
    protected void doExecute(StackContext context, StackEvent payload, Map<Object, Object> variables) throws Exception {
        Stack stack = context.getStack();
        stackUpdater().updateStackStatus(context.getStack(), DetailedStackStatus.REBUILD_IN_PROGRESS, "Updating FreeIPA load balancer");
        sendEvent(context,
                new LoadBalancerUpdateRequest(stack.getId(), context.getCloudContext(), context.getCloudCredential(), context.getCloudStack()));
    }
}
