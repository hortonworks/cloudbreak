package com.sequenceiq.cloudbreak.structuredevent.event.cdp;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.structuredevent.event.rest.RestCallDetails;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CDPStructuredRestCallEvent extends CDPStructuredEvent {
    private RestCallDetails restCall;

    public CDPStructuredRestCallEvent() {
        super(CDPStructuredRestCallEvent.class.getSimpleName());
    }

    public CDPStructuredRestCallEvent(CDPOperationDetails operation, RestCallDetails restCall) {
        super(CDPStructuredRestCallEvent.class.getSimpleName(), operation);
        this.restCall = restCall;
    }

    @Override
    public String getStatus() {
        return restCall.getRestResponse().getStatusText() != null
                ? String.format("%s - %s", restCall.getRestResponse().getStatusText(), restCall.getRestResponse().getStatusCode())
                : Integer.toString(restCall.getRestResponse().getStatusCode());
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
