package com.sequenceiq.freeipa.flow.freeipa.rebuild.action;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.sequenceiq.freeipa.flow.freeipa.upscale.action.FreeIpaUpscaleActions;
import com.sequenceiq.freeipa.flow.stack.StackContext;
import com.sequenceiq.freeipa.flow.stack.StackEvent;
import com.sequenceiq.freeipa.flow.stack.provision.event.clusterproxy.ClusterProxyRegistrationRequest;

/**
 * TODO
 * Register new instance to cluster proxy
 *
 * @see FreeIpaUpscaleActions#updateClusterProxyRegistrationPreBootstrapAction()
 */
@Component("RebuildRegisterClusterProxyAction")
public class RebuildRegisterClusterProxyAction extends AbstractRebuildAction<StackEvent> {
    protected RebuildRegisterClusterProxyAction() {
        super(StackEvent.class);
    }

    @Override
    protected void doExecute(StackContext context, StackEvent payload, Map<Object, Object> variables) throws Exception {
        sendEvent(context, new ClusterProxyRegistrationRequest(payload.getResourceId()));
    }
}
