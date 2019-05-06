package com.sequenceiq.cloudbreak.structuredevent;

import java.util.List;

import com.sequenceiq.cloudbreak.workspace.model.Workspace;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredEvent;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredEventContainer;

public interface StructuredEventService extends StructuredEventSenderService {

    <T extends StructuredEvent> List<T> getEventsForWorkspaceWithType(Workspace workspace, Class<T> eventClass);

    <T extends StructuredEvent> List<T> getEventsForWorkspaceWithTypeSince(Workspace workspace, Class<T> eventClass, Long since);

    <T extends StructuredEvent> List<T> getEventsWithTypeAndResourceId(Class<T> eventClass, String resourceType, Long resourceId);

    StructuredEventContainer getEventsForUserWithResourceId(String resourceType, Long resourceId);

    StructuredEventContainer getStructuredEventsForStack(String name, Long workspaceId);
}
