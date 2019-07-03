package com.sequenceiq.cloudbreak.core.flow2.stack.termination;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.reactor.api.event.recipe.ClusterProxyDeregisterRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.recipe.StackPreTerminationSuccess;

@Component("ClusterProxyDeregisterAction")
public class ClusterProxyDeregisterAction extends AbstractStackTerminationAction<StackPreTerminationSuccess> {

    public ClusterProxyDeregisterAction() {
        super(StackPreTerminationSuccess.class);
    }

    @Override
    protected void doExecute(StackTerminationContext context, StackPreTerminationSuccess payload, Map<Object, Object> variables) {
        ClusterProxyDeregisterRequest deregisterRequest = createRequest(context);
        sendEvent(context, deregisterRequest.selector(), deregisterRequest);
    }

    @Override
    protected ClusterProxyDeregisterRequest createRequest(StackTerminationContext context) {
        return new ClusterProxyDeregisterRequest(context.getStack().getId());
    }
}
