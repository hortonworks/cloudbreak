package com.sequenceiq.cloudbreak.structuredevent;

import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPStructuredEvent;

public interface CDPStructuredEventClient {
    void sendStructuredEvent(CDPStructuredEvent structuredEvent);
}
