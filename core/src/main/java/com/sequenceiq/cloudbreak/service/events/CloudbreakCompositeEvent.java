package com.sequenceiq.cloudbreak.service.events;

import java.io.Serializable;
import java.util.Collection;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredNotificationEvent;

public class CloudbreakCompositeEvent implements Serializable {

    private final ResourceEvent resourceEvent;

    private final Collection<String> resourceEventMessageArgs;

    private final StructuredNotificationEvent structuredNotificationEvent;

    private final StackV4Response stackResponse;

    private final String owner;

    public CloudbreakCompositeEvent(ResourceEvent resourceEvent, Collection<String> resourceEventMessageArgs,
            StructuredNotificationEvent structuredNotificationEvent) {
        this.resourceEvent = resourceEvent;
        this.resourceEventMessageArgs = resourceEventMessageArgs;
        this.structuredNotificationEvent = structuredNotificationEvent;
        this.stackResponse = null;
        this.owner = null;
    }

    public CloudbreakCompositeEvent(ResourceEvent resourceEvent, Collection<String> resourceEventMessageArgs,
            StructuredNotificationEvent structuredNotificationEvent, StackV4Response stackResponse, String owner) {
        this.resourceEvent = resourceEvent;
        this.resourceEventMessageArgs = resourceEventMessageArgs;
        this.structuredNotificationEvent = structuredNotificationEvent;
        this.stackResponse = stackResponse;
        this.owner = owner;
    }

    public ResourceEvent getResourceEvent() {
        return resourceEvent;
    }

    public Collection<String> getResourceEventMessageArgs() {
        return resourceEventMessageArgs;
    }

    public StructuredNotificationEvent getStructuredNotificationEvent() {
        return structuredNotificationEvent;
    }

    public StackV4Response getStackResponse() {
        return stackResponse;
    }

    public String getOwner() {
        return owner;
    }
}
