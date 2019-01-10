package com.sequenceiq.cloudbreak.api.endpoint.v4.flexsubscriptions.responses;

import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import io.swagger.annotations.ApiModel;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
public class FlexSubscriptionV4Responses {

    private Set<FlexSubscriptionV4Response> responses;

    public Set<FlexSubscriptionV4Response> getResponses() {
        return responses;
    }

    public void setResponses(Set<FlexSubscriptionV4Response> responses) {
        this.responses = responses;
    }

    public static FlexSubscriptionV4Responses responses(Set<FlexSubscriptionV4Response> responses) {
        FlexSubscriptionV4Responses v4Responses = new FlexSubscriptionV4Responses();
        v4Responses.setResponses(responses);
        return v4Responses;
    }
}
