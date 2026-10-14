package com.sequenceiq.cloudbreak.structuredevent.event.cdp.freeipa;

import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPOperationDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPStructuredEvent;

public class CDPFreeipaStructuredSyncEvent extends CDPStructuredEvent {

    private FreeIpaStackDetails stackDetails;

    public CDPFreeipaStructuredSyncEvent() {
    }

    public CDPFreeipaStructuredSyncEvent(CDPOperationDetails operation, FreeIpaStackDetails stackDetails) {
        super(CDPFreeipaStructuredSyncEvent.class.getSimpleName(), operation, null, null);
        this.stackDetails = stackDetails;
    }

    @Override
    public String getStatus() {
        return SENT;
    }

    @Override
    public Long getDuration() {
        return ZERO;
    }

    public FreeIpaStackDetails getStackDetails() {
        return stackDetails;
    }

    public void setStackDetails(FreeIpaStackDetails stackDetails) {
        this.stackDetails = stackDetails;
    }
}
