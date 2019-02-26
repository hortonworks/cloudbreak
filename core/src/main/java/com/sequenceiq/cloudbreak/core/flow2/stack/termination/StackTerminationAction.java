package com.sequenceiq.cloudbreak.core.flow2.stack.termination;

import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.event.resource.TerminateStackRequest;
import com.sequenceiq.cloudbreak.domain.stack.cluster.gateway.Gateway;
import com.sequenceiq.cloudbreak.reactor.api.event.recipe.StackPreTerminationSuccess;
import com.sequenceiq.cloudbreak.service.StackUpdater;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.events.CloudbreakEventService;
import com.sequenceiq.cloudbreak.service.messages.CloudbreakMessagesService;
import com.sequenceiq.cloudbreak.service.proxy.ProxyRegistrator;
import com.sequenceiq.cloudbreak.service.stack.flow.TerminationService;

@Component("StackTerminationAction")
public class StackTerminationAction extends AbstractStackTerminationAction<StackPreTerminationSuccess> {
    private static final Logger LOGGER = LoggerFactory.getLogger(StackTerminationAction.class);

    @Inject
    private StackUpdater stackUpdater;

    @Inject
    private CloudbreakMessagesService messagesService;

    @Inject
    private CloudbreakEventService cloudbreakEventService;

    @Inject
    private TerminationService terminationService;

    @Inject
    private ProxyRegistrator proxyRegistrator;

    @Inject
    private ClusterService clusterService;

    public StackTerminationAction() {
        super(StackPreTerminationSuccess.class);
    }

    @Override
    protected void doExecute(StackTerminationContext context, StackPreTerminationSuccess payload, Map<Object, Object> variables) {
        String name = context.getStack().getName();
        if (context.getStack().getCluster() != null) {
            Gateway gateway = context.getStack().getCluster().getGateway();
            if (proxyRegistrator.isKnoxEnabled(gateway)) {
                proxyRegistrator.remove(name);
            }
        }
        doExecute(context);
    }

    @Override
    protected TerminateStackRequest<?> createRequest(StackTerminationContext context) {
        return new TerminateStackRequest<>(context.getCloudContext(), context.getCloudStack(), context.getCloudCredential(), context.getCloudResources());
    }

    protected void doExecute(StackTerminationContext context) {
        TerminateStackRequest<?> terminateRequest = createRequest(context);
        sendEvent(context.getFlowId(), terminateRequest.selector(), terminateRequest);
    }
}
