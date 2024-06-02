package com.sequenceiq.cloudbreak.core.flow2.stack.termination;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.reactor.api.event.recipe.CcmKeyDeregisterSuccess;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.consumption.AttachedVolumeConsumptionCollectionUnschedulingRequest;

@Component("AttachedVolumeConsumptionCollectionUnschedulingAction")
public class AttachedVolumeConsumptionCollectionUnschedulingAction extends AbstractStackTerminationAction<CcmKeyDeregisterSuccess> {

    public AttachedVolumeConsumptionCollectionUnschedulingAction() {
        super(CcmKeyDeregisterSuccess.class);
    }

    @Override
    protected void doExecute(StackTerminationContext context, CcmKeyDeregisterSuccess payload, Map<Object, Object> variables) {
        sendEvent(context);
    }

    @Override
    protected AttachedVolumeConsumptionCollectionUnschedulingRequest createRequest(StackTerminationContext context) {
        return new AttachedVolumeConsumptionCollectionUnschedulingRequest(context.getStack().getId());
    }

}
