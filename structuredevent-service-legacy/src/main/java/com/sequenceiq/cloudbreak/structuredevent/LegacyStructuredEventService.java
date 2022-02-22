package com.sequenceiq.cloudbreak.structuredevent;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.sequenceiq.cloudbreak.structuredevent.event.StructuredEvent;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredEventContainer;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;

public interface LegacyStructuredEventService extends StructuredEventSenderService {

    <T extends StructuredEvent> List<T> getEventsForWorkspaceWithType(Workspace workspace, Class<T> eventClass);

    <T extends StructuredEvent> List<T> getEventsForWorkspaceWithTypeSince(Workspace workspace, Class<T> eventClass, Long since);

    <T extends StructuredEvent> List<T> getEventsWithTypeAndResourceId(Class<T> eventClass, String resourceType, Long resourceId);

    <T extends StructuredEvent> Page<T> getEventsLimitedWithTypeAndResourceId(Class<T> eventClass, String resourceType, Long resourceId, Pageable pageable);

    StructuredEventContainer getEventsForUserWithResourceId(String resourceType, Long resourceId);

    StructuredEventContainer getStructuredEventsForStack(String name, Long workspaceId);

    StructuredEventContainer getStructuredEventsForStackByCrn(String crn, Long workspaceId, boolean onlyAlive);
}
