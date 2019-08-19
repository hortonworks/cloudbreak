package com.sequenceiq.freeipa.flow.stack.termination.action;

import java.util.Map;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus;
import com.sequenceiq.freeipa.flow.stack.termination.StackTerminationContext;
import com.sequenceiq.freeipa.flow.stack.termination.event.TerminationEvent;
import com.sequenceiq.freeipa.flow.stack.termination.event.clusterproxy.ClusterProxyDeregistrationRequest;
import com.sequenceiq.freeipa.service.stack.StackUpdater;

@Component("DeregisterClusterProxyAction")
public class DeregisterClusterProxyAction extends AbstractStackTerminationAction<TerminationEvent> {

    @Inject
    private StackUpdater stackUpdater;

    public DeregisterClusterProxyAction() {
        super(TerminationEvent.class);
    }

    @Override
    protected void doExecute(StackTerminationContext context, TerminationEvent payload, Map<Object, Object> variables) {
        stackUpdater.updateStackStatus(context.getStack().getId(), DetailedStackStatus.DEREGISTERING_WITH_CLUSTERPROXY,
                "Deregistering FreeIPA from Cluster Proxy.");
        ClusterProxyDeregistrationRequest clusterProxyDeregistrationRequest =
                new ClusterProxyDeregistrationRequest(payload.getResourceId(), payload.getForced());
        sendEvent(context, clusterProxyDeregistrationRequest.selector(), clusterProxyDeregistrationRequest);
    }
}
