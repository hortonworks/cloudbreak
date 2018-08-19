package com.sequenceiq.cloudbreak.structuredevent.event;

import com.sequenceiq.cloudbreak.structuredevent.event.rest.RestCallDetails;

public class StructuredRestCallEvent extends StructuredEvent {
    private RestCallDetails restCall;

    public StructuredRestCallEvent() {
    }

    public StructuredRestCallEvent(OperationDetails operation, RestCallDetails restCall, Long orgId, String userId) {
        super(StructuredRestCallEvent.class.getSimpleName(), operation, orgId, userId);
        this.restCall = restCall;
    }

    @Override
    public String getStatus() {
        String status;
        if (restCall.getRestResponse().getStatusText() != null) {
            status = String.format("%s - %s", restCall.getRestResponse().getStatusText(), restCall.getRestResponse().getStatusCode());
        } else {
            status = Integer.toString(restCall.getRestResponse().getStatusCode());
        }
        return status;
    }

    @Override
    public Long getDuration() {
        return restCall.getDuration();
    }

    public RestCallDetails getRestCall() {
        return restCall;
    }

    public void setRestCall(RestCallDetails restCall) {
        this.restCall = restCall;
    }
}
