package com.sequenceiq.cloudbreak.core.flow2.stack.termination;

import static com.sequenceiq.cloudbreak.api.model.Status.DELETE_IN_PROGRESS;

import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.DetailedStackStatus;
import com.sequenceiq.cloudbreak.cloud.event.resource.TerminateStackRequest;
import com.sequenceiq.cloudbreak.core.flow2.stack.Msg;
import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.reactor.api.event.recipe.StackPreTerminationSuccess;
import com.sequenceiq.cloudbreak.repository.StackUpdater;
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
        proxyRegistrator.remove(name);
        doExecute(context);
    }

    @Override
    protected TerminateStackRequest createRequest(StackTerminationContext context) {
        return new TerminateStackRequest<>(context.getCloudContext(), context.getCloudStack(), context.getCloudCredential(), context.getCloudResources());
    }

    protected void doExecute(StackTerminationContext context) {
        TerminateStackRequest terminateRequest = createRequest(context);
        Stack stack = context.getStack();
        putClusterToDeleteInProgressState(stack);
        stackUpdater.updateStackStatus(stack.getId(), DetailedStackStatus.DELETE_IN_PROGRESS, "Terminating the cluster and its infrastructure.");
        cloudbreakEventService.fireCloudbreakEvent(context.getStack().getId(), DELETE_IN_PROGRESS.name(),
                messagesService.getMessage(Msg.STACK_DELETE_IN_PROGRESS.code()));
        LOGGER.debug("Assembling terminate stack event for stack: {}", stack);
        LOGGER.info("Triggering terminate stack event: {}", terminateRequest);
        sendEvent(context.getFlowId(), terminateRequest.selector(), terminateRequest);
    }

    private void putClusterToDeleteInProgressState(Stack stack) {
        Cluster cluster = stack.getCluster();
        if (cluster != null) {
            cluster.setStatus(DELETE_IN_PROGRESS);
            clusterService.updateCluster(cluster);
        }
    }
}
