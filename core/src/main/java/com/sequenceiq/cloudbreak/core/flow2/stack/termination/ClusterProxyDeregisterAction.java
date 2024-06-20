package com.sequenceiq.cloudbreak.core.flow2.stack.termination;

import java.util.Map;

import jakarta.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.clusterproxy.ClusterProxyEnablementService;
import com.sequenceiq.cloudbreak.reactor.api.event.recipe.ClusterProxyDeregisterRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.recipe.ClusterProxyDeregisterSuccess;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.encryption.DeleteUserdataSecretsFinished;

@Component("ClusterProxyDeregisterAction")
public class ClusterProxyDeregisterAction extends AbstractStackTerminationAction<DeleteUserdataSecretsFinished> {
    @Inject
    private ClusterProxyEnablementService clusterProxyEnablementService;

    public ClusterProxyDeregisterAction() {
        super(DeleteUserdataSecretsFinished.class);
    }

    @Override
    protected void doExecute(StackTerminationContext context, DeleteUserdataSecretsFinished payload, Map<Object, Object> variables) {
        if (!context.getTerminationType().isRecovery() && clusterProxyEnablementService.isClusterProxyApplicable(context.getStack().getCloudPlatform())) {
            ClusterProxyDeregisterRequest deregisterRequest = createRequest(context);
            sendEvent(context, deregisterRequest.selector(), deregisterRequest);
        } else {
            ClusterProxyDeregisterSuccess clusterProxyDeregisterSuccess = new ClusterProxyDeregisterSuccess(payload.getResourceId());
            sendEvent(context, clusterProxyDeregisterSuccess);
        }
    }

    @Override
    protected ClusterProxyDeregisterRequest createRequest(StackTerminationContext context) {
        return new ClusterProxyDeregisterRequest(context.getStack().getId(), context.getStack().getCloudPlatform());
    }
}
