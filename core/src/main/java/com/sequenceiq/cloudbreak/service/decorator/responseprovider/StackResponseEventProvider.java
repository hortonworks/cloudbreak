package com.sequenceiq.cloudbreak.service.decorator.responseprovider;

import java.util.List;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.event.CloudbreakEventsJson;
import com.sequenceiq.cloudbreak.api.model.stack.StackResponse;
import com.sequenceiq.cloudbreak.api.model.stack.StackResponseEntries;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.facade.CloudbreakEventsFacade;

@Component
public class StackResponseEventProvider implements ResponseProvider {
    @Inject
    private CloudbreakEventsFacade cloudbreakEventsFacade;

    @Override
    public StackResponse providerEntriesToStackResponse(Stack stack, StackResponse stackResponse) {
        List<CloudbreakEventsJson> cloudbreakEvents = cloudbreakEventsFacade.retrieveEventsByStack(stack.getId());
        stackResponse.setCloudbreakEvents(cloudbreakEvents);
        return stackResponse;
    }

    @Override
    public String type() {
        return StackResponseEntries.EVENTS.getEntryName();
    }
}
