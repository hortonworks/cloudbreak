package com.sequenceiq.cloudbreak.structuredevent;

import com.sequenceiq.cloudbreak.structuredevent.event.StructuredEvent;

public interface StructuredEventClient {
    void sendStructuredEvent(StructuredEvent structuredEvent);
}
