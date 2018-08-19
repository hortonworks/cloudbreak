package com.sequenceiq.cloudbreak.structuredevent;

import java.util.List;

import com.sequenceiq.cloudbreak.domain.organization.Organization;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredEvent;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredEventContainer;

public interface StructuredEventService extends StructuredEventSenderService {

    <T extends StructuredEvent> List<T> getEventsForOrgWithType(Organization organization, Class<T> eventClass);

    <T extends StructuredEvent> List<T> getEventsForOrgWithTypeSince(Organization organization, Class<T> eventClass, Long since);

    <T extends StructuredEvent> List<T> getEventsWithTypeAndResourceId(Class<T> eventClass, String resourceType, Long resourceId);

    StructuredEventContainer getEventsForUserWithResourceId(String resourceType, Long resourceId);
}
