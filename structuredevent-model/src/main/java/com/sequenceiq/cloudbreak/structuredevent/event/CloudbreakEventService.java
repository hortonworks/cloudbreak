package com.sequenceiq.cloudbreak.structuredevent.event;

import java.util.Collection;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.sequenceiq.cloudbreak.event.ResourceEvent;

public interface CloudbreakEventService {

    void fireCloudbreakEvent(Long entityId, String eventType, ResourceEvent resourceEvent);

    void fireCloudbreakEvent(Long entityId, String eventType, ResourceEvent resourceEvent, Collection<String> eventMessageArgs);

    void fireCloudbreakInstanceGroupEvent(Long stackId, String eventType, String instanceGroupName, ResourceEvent resourceEvent,
            Collection<String> eventMessageArgs);

    List<StructuredNotificationEvent> cloudbreakEvents(Long workspaceId, Long since);

    List<StructuredNotificationEvent> cloudbreakEventsForStack(Long stackId);

    Page<StructuredNotificationEvent> cloudbreakEventsForStack(Long stackId, Pageable pageable);
}
