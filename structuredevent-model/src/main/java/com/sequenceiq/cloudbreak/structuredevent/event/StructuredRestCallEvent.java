package com.sequenceiq.cloudbreak.structuredevent.event;

import com.sequenceiq.cloudbreak.structuredevent.event.rest.RestCallDetails;

public class StructuredRestCallEvent extends StructuredEvent {
    private RestCallDetails restCall;

    public StructuredRestCallEvent() {
    }

    public StructuredRestCallEvent(OperationDetails operation, RestCallDetails restCall) {
        super(StructuredRestCallEvent.class.getSimpleName(), operation);
        this.restCall = restCall;
    }

    public RestCallDetails getRestCall() {
        return restCall;
    }

    public void setRestCall(RestCallDetails restCall) {
        this.restCall = restCall;
    }
}
