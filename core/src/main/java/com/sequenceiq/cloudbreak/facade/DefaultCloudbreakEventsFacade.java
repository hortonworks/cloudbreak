package com.sequenceiq.cloudbreak.facade;

import com.sequenceiq.cloudbreak.api.model.CloudbreakEventsJson;
import com.sequenceiq.cloudbreak.service.events.CloudbreakEventService;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredNotificationEvent;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.List;

@Service
public class DefaultCloudbreakEventsFacade implements CloudbreakEventsFacade {
    @Inject
    private CloudbreakEventService cloudbreakEventService;

    @Inject
    @Qualifier("conversionService")
    private ConversionService conversionService;

    @Override
    public List<CloudbreakEventsJson> retrieveEvents(String owner, Long since) {
        List<StructuredNotificationEvent> cloudbreakEvents = cloudbreakEventService.cloudbreakEvents(owner, since);
        return (List<CloudbreakEventsJson>) conversionService
                .convert(cloudbreakEvents, TypeDescriptor.forObject(cloudbreakEvents), TypeDescriptor.collection(List.class,
                        TypeDescriptor.valueOf(CloudbreakEventsJson.class)));
    }

    @Override
    public List<CloudbreakEventsJson> retrieveEventsByStack(String owner, Long stackId) {
        List<StructuredNotificationEvent> cloudbreakEvents = cloudbreakEventService.cloudbreakEventsForStack(owner, stackId);
        return (List<CloudbreakEventsJson>) conversionService
                .convert(cloudbreakEvents, TypeDescriptor.forObject(cloudbreakEvents), TypeDescriptor.collection(List.class,
                        TypeDescriptor.valueOf(CloudbreakEventsJson.class)));
    }
}
