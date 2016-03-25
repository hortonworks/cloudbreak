package com.sequenceiq.cloudbreak.core.flow2.stack.stop;

import java.util.Map;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.Status;
import com.sequenceiq.cloudbreak.cloud.event.instance.StopInstancesResult;
import com.sequenceiq.cloudbreak.common.type.BillingStatus;
import com.sequenceiq.cloudbreak.core.flow2.stack.Msg;
import com.sequenceiq.cloudbreak.core.flow2.stack.start.StackStartStopContext;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.repository.StackUpdater;
import com.sequenceiq.cloudbreak.service.cluster.flow.EmailSenderService;

@Component("StackStopFinishedAction")
public class StackStopFinishedAction extends AbstractStackStopAction<StopInstancesResult> {
    @Inject
    private StackUpdater stackUpdater;
    @Inject
    private EmailSenderService emailSenderService;

    public StackStopFinishedAction() {
        super(StopInstancesResult.class);
    }

    @Override
    protected Long getStackId(StopInstancesResult payload) {
        return payload.getCloudContext().getId();
    }

    @Override
    protected void doExecute(StackStartStopContext context, StopInstancesResult payload, Map<Object, Object> variables) throws Exception {
        Stack stack = context.getStack();
        stackUpdater.updateStackStatus(stack.getId(), Status.STOPPED, "Cluster infrastructure stopped successfully.");

        fireEventAndLog(stack.getId(), context, Msg.STACK_INFRASTRUCTURE_STOPPED, Status.STOPPED.name());
        fireEventAndLog(stack.getId(), context, Msg.STACK_BILLING_STOPPED, BillingStatus.BILLING_STOPPED.name());

        if (stack.getCluster() != null && stack.getCluster().getEmailNeeded()) {
            emailSenderService.sendStopSuccessEmail(stack.getCluster().getOwner(), stack.getAmbariIp(), stack.getCluster().getName());
            fireEventAndLog(stack.getId(), context, Msg.STACK_NOTIFICATION_EMAIL, Status.STOPPED.name());
        }
        sendEvent(context.getFlowId(), StackStopEvent.STOP_FINALIZED_EVENT.stringRepresentation(), null);
    }
}
