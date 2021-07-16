package com.sequenceiq.cloudbreak.core.flow2.stack.termination;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.DELETE_IN_PROGRESS;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.UPDATE_IN_PROGRESS;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.DATALAKE_RECOVERY_IN_PROGRESS;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.STACK_DELETE_IN_PROGRESS;

import java.util.Map;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.reactor.api.event.recipe.StackPreTerminationFailed;
import com.sequenceiq.cloudbreak.reactor.api.event.recipe.StackPreTerminationRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.TerminationEvent;
import com.sequenceiq.cloudbreak.service.StackUpdater;
import com.sequenceiq.cloudbreak.structuredevent.event.CloudbreakEventService;

@Component("StackPreTerminationAction")
public class StackPreTerminationAction extends AbstractStackTerminationAction<TerminationEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackPreTerminationAction.class);

    @Inject
    private StackUpdater stackUpdater;

    @Inject
    private CloudbreakEventService cloudbreakEventService;

    public StackPreTerminationAction() {
        super(TerminationEvent.class);
    }

    @Override
    protected void prepareExecution(TerminationEvent payload, Map<Object, Object> variables) {
        variables.put(TERMINATION_TYPE, payload.getTerminationType());
    }

    @Override
    protected void doExecute(StackTerminationContext context, TerminationEvent payload, Map<Object, Object> variables) {
        Stack stack = context.getStack();
        if (stack == null || StringUtils.isEmpty(stack.getEnvironmentCrn())) {
            LOGGER.info("Could not trigger stack event on null, {}", payload);
            String statusReason = "Stack or environment not found.";
            StackPreTerminationFailed terminateStackResult = new StackPreTerminationFailed(payload.getResourceId(), new IllegalArgumentException(statusReason));
            sendEvent(context, StackTerminationEvent.PRE_TERMINATION_FAILED_EVENT.event(), terminateStackResult);
        } else {
            updateStatus(context, payload, stack);
            sendEvent(context);
        }
    }

    private void updateStatus(StackTerminationContext context, TerminationEvent payload, Stack stack) {
        if (payload.getTerminationType().isRecovery()) {
            stackUpdater.updateStackStatus(stack.getId(), DetailedStackStatus.CLUSTER_RECOVERY_IN_PROGRESS, "Recovering the cluster and its infrastructure.");
            cloudbreakEventService.fireCloudbreakEvent(context.getStack().getId(), UPDATE_IN_PROGRESS.name(), DATALAKE_RECOVERY_IN_PROGRESS);
            LOGGER.debug("Assembling recovery stack event for stack: {}", stack);
            LOGGER.debug("Triggering recovery stack event: {}", payload);
        } else {
            stackUpdater.updateStackStatus(stack.getId(), DetailedStackStatus.DELETE_IN_PROGRESS, "Terminating the cluster and its infrastructure.");
            cloudbreakEventService.fireCloudbreakEvent(context.getStack().getId(), DELETE_IN_PROGRESS.name(), STACK_DELETE_IN_PROGRESS);
            LOGGER.debug("Assembling terminate stack event for stack: {}", stack);
            LOGGER.debug("Triggering terminate stack event: {}", payload);
        }
    }

    @Override
    protected StackPreTerminationRequest createRequest(StackTerminationContext context) {
        return new StackPreTerminationRequest(context.getStack().getId(), context.getTerminationType().isForced());
    }
}
