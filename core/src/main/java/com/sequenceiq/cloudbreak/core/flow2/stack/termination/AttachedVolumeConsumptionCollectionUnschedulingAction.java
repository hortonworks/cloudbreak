package com.sequenceiq.cloudbreak.core.flow2.stack.termination;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.DELETE_IN_PROGRESS;

import java.util.Map;

import jakarta.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.recipe.CcmKeyDeregisterSuccess;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.consumption.AttachedVolumeConsumptionCollectionUnschedulingRequest;
import com.sequenceiq.cloudbreak.structuredevent.event.CloudbreakEventService;

@Component("AttachedVolumeConsumptionCollectionUnschedulingAction")
public class AttachedVolumeConsumptionCollectionUnschedulingAction extends AbstractStackTerminationAction<CcmKeyDeregisterSuccess> {

    @Inject
    private CloudbreakEventService cloudbreakEventService;

    public AttachedVolumeConsumptionCollectionUnschedulingAction() {
        super(CcmKeyDeregisterSuccess.class);
    }

    @Override
    protected void doExecute(StackTerminationContext context, CcmKeyDeregisterSuccess payload, Map<Object, Object> variables) {
        cloudbreakEventService.fireCloudbreakEvent(payload.getResourceId(), DELETE_IN_PROGRESS.name(),
                ResourceEvent.STACK_ATTACHED_VOLUME_CONSUMPTION_COLLECTION_UNSCHEDULING_STARTED);
        sendEvent(context);
    }

    @Override
    protected AttachedVolumeConsumptionCollectionUnschedulingRequest createRequest(StackTerminationContext context) {
        return new AttachedVolumeConsumptionCollectionUnschedulingRequest(context.getStack().getId());
    }

}
