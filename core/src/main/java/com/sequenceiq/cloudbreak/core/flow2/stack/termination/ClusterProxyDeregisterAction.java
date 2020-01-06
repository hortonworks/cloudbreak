package com.sequenceiq.cloudbreak.core.flow2.stack.termination;

import java.util.Map;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.clusterproxy.ClusterProxyConfiguration;
import com.sequenceiq.cloudbreak.reactor.api.event.recipe.ClusterProxyDeregisterRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.recipe.ClusterProxyDeregisterSuccess;
import com.sequenceiq.cloudbreak.reactor.api.event.recipe.StackPreTerminationSuccess;

@Component("ClusterProxyDeregisterAction")
public class ClusterProxyDeregisterAction extends AbstractStackTerminationAction<StackPreTerminationSuccess> {
    @Inject
    private ClusterProxyConfiguration clusterProxyConfiguration;

    public ClusterProxyDeregisterAction() {
        super(StackPreTerminationSuccess.class);
    }

    @Override
    protected void doExecute(StackTerminationContext context, StackPreTerminationSuccess payload, Map<Object, Object> variables) {
        if (clusterProxyConfiguration.isClusterProxyIntegrationEnabled()) {
            ClusterProxyDeregisterRequest deregisterRequest = createRequest(context);
            sendEvent(context, deregisterRequest.selector(), deregisterRequest);
        } else {
            ClusterProxyDeregisterSuccess clusterProxyDeregisterSuccess = new ClusterProxyDeregisterSuccess(payload.getResourceId());
            sendEvent(context, clusterProxyDeregisterSuccess);
        }
    }

    @Override
    protected ClusterProxyDeregisterRequest createRequest(StackTerminationContext context) {
        return new ClusterProxyDeregisterRequest(context.getStack().getId());
    }
}
