package com.sequenceiq.cloudbreak.facade;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.controller.json.CloudbreakEventsJson;
import com.sequenceiq.cloudbreak.domain.CloudbreakEvent;
import com.sequenceiq.cloudbreak.service.events.CloudbreakEventService;

@Service
public class DefaultCloudbreakEventsFacade implements CloudbreakEventsFacade {

    @Autowired
    private CloudbreakEventService cloudbreakEventService;

    @Autowired
    @Qualifier("conversionService")
    private ConversionService conversionService;

    @Override
    public List<CloudbreakEventsJson> retrieveEvents(String owner, Long since) {
        List<CloudbreakEvent> cloudbreakEvents = cloudbreakEventService.cloudbreakEvents(owner, since);
        return (List<CloudbreakEventsJson>) conversionService
                .convert(cloudbreakEvents, TypeDescriptor.forObject(cloudbreakEvents), TypeDescriptor.collection(List.class,
                        TypeDescriptor.valueOf(CloudbreakEventsJson.class)));
    }
}
