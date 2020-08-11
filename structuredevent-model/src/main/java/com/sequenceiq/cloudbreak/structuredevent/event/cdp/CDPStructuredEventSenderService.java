package com.sequenceiq.cloudbreak.structuredevent.event.cdp;

public interface CDPStructuredEventSenderService {
    void create(CDPStructuredEvent structuredEvent);

    boolean isEnabled();
}
