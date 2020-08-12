package com.sequenceiq.cloudbreak.structuredevent.service;

import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPStructuredEvent;

public interface CDPStructuredEventClient {
    void sendStructuredEvent(CDPStructuredEvent structuredEvent);
}
