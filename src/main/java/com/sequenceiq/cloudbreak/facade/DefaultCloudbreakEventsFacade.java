package com.sequenceiq.cloudbreak.facade;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.controller.json.CloudbreakEventsJson;
import com.sequenceiq.cloudbreak.converter.CloudbreakEventConverter;
import com.sequenceiq.cloudbreak.domain.CloudbreakEvent;
import com.sequenceiq.cloudbreak.service.events.CloudbreakEventService;

@Service
public class DefaultCloudbreakEventsFacade implements CloudbreakEventsFacade {
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultCloudbreakEventsFacade.class);

    @Autowired
    private CloudbreakEventService cloudbreakEventService;

    @Autowired
    private CloudbreakEventConverter eventConverter;

    @Override
    public List<CloudbreakEventsJson> retrieveEvents(Long userId, long since) {
        List<CloudbreakEvent> cloudbreakEvents = cloudbreakEventService.cloudbreakEvents(userId, since);
        return new ArrayList(eventConverter.convertAllEntityToJson(cloudbreakEvents));
    }
}
