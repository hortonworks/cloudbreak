package com.sequenceiq.cloudbreak.structuredevent.event.cdp.freeipa;

import com.sequenceiq.cloudbreak.structuredevent.event.StackDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPOperationDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPStructuredEvent;

public class CDPFreeipaStructuredSyncEvent extends CDPStructuredEvent {

    private StackDetails stackDetails;

    public CDPFreeipaStructuredSyncEvent() {
    }

    public CDPFreeipaStructuredSyncEvent(CDPOperationDetails operation, StackDetails stackDetails) {
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

    public StackDetails getStackDetails() {
        return stackDetails;
    }

    public void setStackDetails(StackDetails stackDetails) {
        this.stackDetails = stackDetails;
    }
}
