package com.sequenceiq.cloudbreak.structuredevent.event.cdp.environment;

import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPOperationDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPStructuredEvent;

public class CDPEnvironmentStructuredSyncEvent extends CDPStructuredEvent {

    private EnvironmentDetails environmentDetails;

    public CDPEnvironmentStructuredSyncEvent() {
    }

    public CDPEnvironmentStructuredSyncEvent(CDPOperationDetails operation, EnvironmentDetails environmentDetails) {
        super(CDPEnvironmentStructuredSyncEvent.class.getSimpleName(), operation, null, null);
        this.environmentDetails = environmentDetails;
    }

    @Override
    public String getStatus() {
        return SENT;
    }

    @Override
    public Long getDuration() {
        return ZERO;
    }

    public EnvironmentDetails getEnvironmentDetails() {
        return environmentDetails;
    }

    public void setEnvironmentDetails(EnvironmentDetails environmentDetails) {
        this.environmentDetails = environmentDetails;
    }
}
