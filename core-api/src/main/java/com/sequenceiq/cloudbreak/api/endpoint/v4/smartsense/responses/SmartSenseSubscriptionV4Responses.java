package com.sequenceiq.cloudbreak.api.endpoint.v4.smartsense.responses;

import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.api.model.JsonEntity;

import io.swagger.annotations.ApiModel;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
public class SmartSenseSubscriptionV4Responses implements JsonEntity {

    private Set<SmartSenseSubscriptionV4Response> responseSet;

    public Set<SmartSenseSubscriptionV4Response> getResponseSet() {
        return responseSet;
    }

    public void setResponseSet(Set<SmartSenseSubscriptionV4Response> responseSet) {
        this.responseSet = responseSet;
    }

    public static SmartSenseSubscriptionV4Responses responses(Set<SmartSenseSubscriptionV4Response> responses) {
        SmartSenseSubscriptionV4Responses v4Responses = new SmartSenseSubscriptionV4Responses();
        v4Responses.setResponseSet(responses);
        return v4Responses;
    }
}
