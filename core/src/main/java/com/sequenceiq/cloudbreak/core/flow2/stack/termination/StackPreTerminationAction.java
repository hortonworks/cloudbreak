package com.sequenceiq.cloudbreak.core.flow2.stack.termination;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.DELETE_IN_PROGRESS;

import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.message.Msg;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.reactor.api.event.recipe.StackPreTerminationFailed;
import com.sequenceiq.cloudbreak.reactor.api.event.recipe.StackPreTerminationRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.TerminationEvent;
import com.sequenceiq.cloudbreak.service.StackUpdater;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.event.CloudbreakEventService;
import com.sequenceiq.cloudbreak.message.CloudbreakMessagesService;

@Component("StackPreTerminationAction")
public class StackPreTerminationAction extends AbstractStackTerminationAction<TerminationEvent> {
    private static final Logger LOGGER = LoggerFactory.getLogger(StackPreTerminationAction.class);

    @Inject
    private StackUpdater stackUpdater;

    @Inject
    private CloudbreakMessagesService messagesService;

    @Inject
    private CloudbreakEventService cloudbreakEventService;

    @Inject
    private ClusterService clusterService;

    public StackPreTerminationAction() {
        super(TerminationEvent.class);
    }

    @Override
    protected void prepareExecution(TerminationEvent payload, Map<Object, Object> variables) {
        variables.put("FORCEDTERMINATION", payload.getForced());
        variables.put("DELETEDEPENDENCIES", payload.getDeleteDependencies());
    }

    @Override
    protected void doExecute(StackTerminationContext context, TerminationEvent payload, Map<Object, Object> variables) {
        Stack stack = context.getStack();
        if (stack == null || stack.getCredential() == null) {
            LOGGER.info("Could not trigger stack event on null, {}", payload);
            String statusReason = "Stack or credential not found.";
            StackPreTerminationFailed terminateStackResult = new StackPreTerminationFailed(payload.getStackId(), new IllegalArgumentException(statusReason));
            sendEvent(context.getFlowId(), StackTerminationEvent.TERMINATION_FAILED_EVENT.event(), terminateStackResult);
        } else {
            putClusterToDeleteInProgressState(stack);
            stackUpdater.updateStackStatus(stack.getId(), DetailedStackStatus.DELETE_IN_PROGRESS, "Terminating the cluster and its infrastructure.");
            cloudbreakEventService.fireCloudbreakEvent(context.getStack().getId(), DELETE_IN_PROGRESS.name(),
                    messagesService.getMessage(Msg.STACK_DELETE_IN_PROGRESS.code()));
            sendEvent(context);
            LOGGER.debug("Assembling terminate stack event for stack: {}", stack);
            LOGGER.debug("Triggering terminate stack event: {}", payload);
        }
    }

    @Override
    protected StackPreTerminationRequest createRequest(StackTerminationContext context) {
        return new StackPreTerminationRequest(context.getStack().getId());
    }

    private void putClusterToDeleteInProgressState(Stack stack) {
        Cluster cluster = stack.getCluster();
        if (cluster != null) {
            cluster.setStatus(DELETE_IN_PROGRESS);
            clusterService.updateCluster(cluster);
        }
    }
}
