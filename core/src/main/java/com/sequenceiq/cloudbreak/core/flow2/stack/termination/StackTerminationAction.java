package com.sequenceiq.cloudbreak.core.flow2.stack.termination;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.event.resource.TerminateStackRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.consumption.AttachedVolumeConsumptionCollectionUnschedulingSuccess;

@Component("StackTerminationAction")
public class StackTerminationAction extends AbstractStackTerminationAction<AttachedVolumeConsumptionCollectionUnschedulingSuccess> {

    public StackTerminationAction() {
        super(AttachedVolumeConsumptionCollectionUnschedulingSuccess.class);
    }

    @Override
    protected void doExecute(StackTerminationContext context, AttachedVolumeConsumptionCollectionUnschedulingSuccess payload, Map<Object, Object> variables) {
        TerminateStackRequest<?> terminateRequest = createRequest(context);
        sendEvent(context, terminateRequest.selector(), terminateRequest);
    }

    @Override
    protected TerminateStackRequest<?> createRequest(StackTerminationContext context) {
        return new TerminateStackRequest<>(context.getCloudContext(), context.getCloudStack(), context.getCloudCredential(), context.getCloudResources());
    }

}
