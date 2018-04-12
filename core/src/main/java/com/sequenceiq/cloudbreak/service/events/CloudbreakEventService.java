package com.sequenceiq.cloudbreak.service.events;

import com.sequenceiq.cloudbreak.structuredevent.event.StructuredNotificationEvent;

import java.util.List;

public interface CloudbreakEventService {

    void fireCloudbreakEvent(Long entityId, String eventType, String eventMessage);

    void fireCloudbreakInstanceGroupEvent(Long stackId, String eventType, String eventMessage, String instanceGroupName);

    List<StructuredNotificationEvent> cloudbreakEvents(String user, Long since);

    List<StructuredNotificationEvent> cloudbreakEventsForStack(String user, Long stackId);
}
