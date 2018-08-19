package com.sequenceiq.cloudbreak.facade;

import java.util.List;

import javax.inject.Inject;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.model.CloudbreakEventsJson;
import com.sequenceiq.cloudbreak.domain.organization.Organization;
import com.sequenceiq.cloudbreak.service.events.CloudbreakEventService;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredNotificationEvent;

@Service
public class DefaultCloudbreakEventsFacade implements CloudbreakEventsFacade {
    @Inject
    private CloudbreakEventService cloudbreakEventService;

    @Inject
    @Qualifier("conversionService")
    private ConversionService conversionService;

    @Override
    public List<CloudbreakEventsJson> retrieveEventsForOrganiztion(Organization organization, Long since) {
        List<StructuredNotificationEvent> cloudbreakEvents = cloudbreakEventService.cloudbreakEvents(organization, since);
        return (List<CloudbreakEventsJson>) conversionService
                .convert(cloudbreakEvents, TypeDescriptor.forObject(cloudbreakEvents), TypeDescriptor.collection(List.class,
                        TypeDescriptor.valueOf(CloudbreakEventsJson.class)));
    }

    @Override
    public List<CloudbreakEventsJson> retrieveEventsByStack(Long stackId) {
        List<StructuredNotificationEvent> cloudbreakEvents = cloudbreakEventService.cloudbreakEventsForStack(stackId);
        return (List<CloudbreakEventsJson>) conversionService
                .convert(cloudbreakEvents, TypeDescriptor.forObject(cloudbreakEvents), TypeDescriptor.collection(List.class,
                        TypeDescriptor.valueOf(CloudbreakEventsJson.class)));
    }
}
