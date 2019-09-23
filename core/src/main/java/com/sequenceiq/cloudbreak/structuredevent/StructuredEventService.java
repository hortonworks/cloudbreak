package com.sequenceiq.cloudbreak.structuredevent;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.sequenceiq.cloudbreak.domain.workspace.Workspace;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredEvent;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredEventContainer;

public interface StructuredEventService extends StructuredEventSenderService {

    <T extends StructuredEvent> List<T> getEventsForWorkspaceWithType(Workspace workspace, Class<T> eventClass);

    <T extends StructuredEvent> List<T> getEventsForWorkspaceWithTypeSince(Workspace workspace, Class<T> eventClass, Long since);

    <T extends StructuredEvent> List<T> getEventsWithTypeAndResourceId(Class<T> eventClass, String resourceType, Long resourceId);

    <T extends StructuredEvent> Page<T> getEventsLimitedWithTypeAndResourceId(Class<T> eventClass, String resourceType, Long resourceId, Pageable pageable);

    StructuredEventContainer getEventsForUserWithResourceId(String resourceType, Long resourceId);
}
