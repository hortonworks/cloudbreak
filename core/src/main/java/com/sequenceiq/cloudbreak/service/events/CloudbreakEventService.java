package com.sequenceiq.cloudbreak.service.events;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.sequenceiq.cloudbreak.domain.workspace.Workspace;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredNotificationEvent;

public interface CloudbreakEventService {

    void fireCloudbreakEvent(Long entityId, String eventType, String eventMessage);

    void fireCloudbreakInstanceGroupEvent(Long stackId, String eventType, String eventMessage, String instanceGroupName);

    List<StructuredNotificationEvent> cloudbreakEvents(Workspace workspace, Long since);

    Page<StructuredNotificationEvent> cloudbreakEventsForStack(Long stackId, Pageable pageable);
}
