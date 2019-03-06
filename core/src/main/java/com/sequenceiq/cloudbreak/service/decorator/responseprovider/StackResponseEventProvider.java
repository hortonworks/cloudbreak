package com.sequenceiq.cloudbreak.service.decorator.responseprovider;

import java.util.List;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.events.responses.CloudbreakEventV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.StackResponseEntries;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.facade.CloudbreakEventsFacade;

@Component
public class StackResponseEventProvider implements ResponseProvider {
    @Inject
    private CloudbreakEventsFacade cloudbreakEventsFacade;

    @Override
    public StackV4Response providerEntriesToStackResponse(Stack stack, StackV4Response stackResponse) {
        List<CloudbreakEventV4Response> cloudbreakEvents = cloudbreakEventsFacade.retrieveEventsByStack(stack.getId());
        stackResponse.setCloudbreakEvents(cloudbreakEvents);
        return stackResponse;
    }

    @Override
    public String type() {
        return StackResponseEntries.EVENTS.getEntryName();
    }
}
