package com.sequenceiq.cloudbreak.structuredevent;

import java.util.List;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.structuredevent.event.StructuredEvent;

@Service
public class DefaultStructuredEventClient implements StructuredEventClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultStructuredEventClient.class);

    @Inject
    private List<StructuredEventSenderService> structuredEventServices;

    @Override
    public void sendStructuredEvent(StructuredEvent structuredEvent) {
        for (StructuredEventSenderService structuredEventSenderService : structuredEventServices) {
            LOGGER.trace("Send event {} with eventsender {}", structuredEvent, structuredEventSenderService.getClass());
            if (structuredEventSenderService.isEnabled()) {
                structuredEventSenderService.create(structuredEvent);
            }
        }
    }
}
