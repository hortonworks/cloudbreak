package com.sequenceiq.cloudbreak.structuredevent.event.rest;

import java.io.Serializable;

public class RestCallDetails implements Serializable {

    private RestRequestDetails restRequest;

    private RestResponseDetails restResponse;

    private Long duration;

    public RestRequestDetails getRestRequest() {
        return restRequest;
    }

    public void setRestRequest(RestRequestDetails restRequest) {
        this.restRequest = restRequest;
    }

    public RestResponseDetails getRestResponse() {
        return restResponse;
    }

    public void setRestResponse(RestResponseDetails restResponse) {
        this.restResponse = restResponse;
    }

    public Long getDuration() {
        return duration;
    }

    public void setDuration(Long duration) {
        this.duration = duration;
    }
}
