package com.sequenceiq.cloudbreak.structuredevent;

import com.sequenceiq.cloudbreak.structuredevent.event.StructuredEvent;

public interface LegacyBaseStructuredEventClient {
    void sendStructuredEvent(StructuredEvent structuredEvent);
}
