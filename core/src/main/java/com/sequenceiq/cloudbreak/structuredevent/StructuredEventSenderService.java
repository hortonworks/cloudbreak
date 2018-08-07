package com.sequenceiq.cloudbreak.structuredevent;

import com.sequenceiq.cloudbreak.structuredevent.event.StructuredEvent;

public interface StructuredEventSenderService {
    void storeStructuredEvent(StructuredEvent structuredEvent);

    boolean isEnabled();
}
