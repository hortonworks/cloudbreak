package com.sequenceiq.cloudbreak.structuredevent;

import com.sequenceiq.cloudbreak.structuredevent.event.StructuredEvent;

public interface StructuredEventService {
    void storeStructuredEvent(StructuredEvent structuredEvent);
}
