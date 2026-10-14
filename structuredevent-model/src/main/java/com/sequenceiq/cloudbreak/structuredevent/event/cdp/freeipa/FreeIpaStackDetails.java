package com.sequenceiq.cloudbreak.structuredevent.event.cdp.freeipa;

import com.sequenceiq.cloudbreak.structuredevent.event.StackDetails;

public class FreeIpaStackDetails extends StackDetails {

    private String trustRelationshipType;

    public String getTrustRelationshipType() {
        return trustRelationshipType;
    }

    public void setTrustRelationshipType(String trustRelationshipType) {
        this.trustRelationshipType = trustRelationshipType;
    }
}
