package com.sequenceiq.cloudbreak.structuredevent.event;

import java.util.Collection;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.sequenceiq.cloudbreak.event.ResourceEvent;

public interface CloudbreakEventService {

    String DATAHUB_RESOURCE_TYPE = "datahub";

    String DATALAKE_RESOURCE_TYPE = "datalake";

    String LEGACY_RESOURCE_TYPE = "stacks";

    void fireCloudbreakEvent(Long entityId, String eventType, ResourceEvent resourceEvent);

    void fireCloudbreakEvent(Long entityId, String eventType, ResourceEvent resourceEvent, Collection<String> eventMessageArgs);

    void fireCloudbreakInstanceGroupEvent(Long entityId, String eventType, String instanceGroupName, ResourceEvent resourceEvent,
            Collection<String> eventMessageArgs);

    List<StructuredNotificationEvent> cloudbreakEvents(Long workspaceId, Long since);

    List<StructuredNotificationEvent> cloudbreakEventsForStack(Long entityId);

    Page<StructuredNotificationEvent> cloudbreakEventsForStack(Long entityId, String type, Pageable pageable);
}
