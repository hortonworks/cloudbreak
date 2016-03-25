package com.sequenceiq.cloudbreak.core.flow2.stack.start;

import static com.sequenceiq.cloudbreak.api.model.Status.AVAILABLE;
import static com.sequenceiq.cloudbreak.cloud.model.Platform.platform;

import java.util.Map;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.event.instance.StartInstancesResult;
import com.sequenceiq.cloudbreak.common.type.BillingStatus;
import com.sequenceiq.cloudbreak.core.flow.FlowPhases;
import com.sequenceiq.cloudbreak.core.flow.context.StackStatusUpdateContext;
import com.sequenceiq.cloudbreak.core.flow2.stack.Msg;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.repository.StackUpdater;

@Component("StackStartFinishedAction")
public class StackStartFinishedAction extends AbstractStackStartAction<StartInstancesResult> {

    @Inject
    private StackUpdater stackUpdater;

    public StackStartFinishedAction() {
        super(StartInstancesResult.class);
    }

    @Override
    protected Long getStackId(StartInstancesResult payload) {
        return payload.getCloudContext().getId();
    }

    @Override
    protected void doExecute(StackStartStopContext context, StartInstancesResult payload, Map<Object, Object> variables) throws Exception {
        Stack stack = context.getStack();
        stackUpdater.updateStackStatus(stack.getId(), AVAILABLE, "Cluster infrastructure started successfully.");
        fireEventAndLog(stack.getId(), context, Msg.STACK_INFRASTRUCTURE_STARTED, AVAILABLE.name());
        fireEventAndLog(stack.getId(), context, Msg.STACK_BILLING_STARTED, BillingStatus.BILLING_STARTED.name());
        sendEvent(context.getFlowId(), StackStartEvent.START_FINALIZED_EVENT.stringRepresentation(), null);
        sendEvent(context.getFlowId(), FlowPhases.METADATA_COLLECT.name(), new StackStatusUpdateContext(stack.getId(), platform(stack.cloudPlatform()), true));
    }
}
