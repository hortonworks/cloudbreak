package com.sequenceiq.cloudbreak.structuredevent;

import com.sequenceiq.cloudbreak.structuredevent.event.StructuredEvent;

public interface LegacyStructuredEventClient {
    void sendStructuredEvent(StructuredEvent structuredEvent);
}
