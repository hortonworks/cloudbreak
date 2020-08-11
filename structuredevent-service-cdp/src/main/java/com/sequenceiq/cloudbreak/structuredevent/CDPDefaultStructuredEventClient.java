package com.sequenceiq.cloudbreak.structuredevent;

import java.util.List;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPStructuredEvent;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPStructuredEventSenderService;

@Service
public class CDPDefaultStructuredEventClient implements CDPStructuredEventClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(CDPDefaultStructuredEventClient.class);

    @Inject
    private List<CDPStructuredEventSenderService> structuredEventServices;

    @Override
    public void sendStructuredEvent(CDPStructuredEvent structuredEvent) {
        for (CDPStructuredEventSenderService structuredEventSenderService : structuredEventServices) {
            LOGGER.trace("Send event {} with eventsender {}", structuredEvent, structuredEventSenderService.getClass());
            if (structuredEventSenderService.isEnabled()) {
                structuredEventSenderService.create(structuredEvent);
            }
        }
    }
}
