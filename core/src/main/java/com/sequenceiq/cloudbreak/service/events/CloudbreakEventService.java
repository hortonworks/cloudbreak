package com.sequenceiq.cloudbreak.service.events;

import java.util.List;

import com.sequenceiq.cloudbreak.domain.organization.Organization;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredNotificationEvent;

public interface CloudbreakEventService {

    void fireCloudbreakEvent(Long entityId, String eventType, String eventMessage);

    void fireCloudbreakInstanceGroupEvent(Long stackId, String eventType, String eventMessage, String instanceGroupName);

    List<StructuredNotificationEvent> cloudbreakEvents(Organization organization, Long since);

    List<StructuredNotificationEvent> cloudbreakEventsForStack(Long stackId);
}
