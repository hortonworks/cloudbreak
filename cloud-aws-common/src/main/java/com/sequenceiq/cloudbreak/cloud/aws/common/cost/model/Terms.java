package com.sequenceiq.cloudbreak.cloud.aws.common.cost.model;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Terms {

    @JsonProperty("OnDemand")
    private Map<String, OfferTerm> onDemand;

    @JsonProperty("Reserved")
    private Map<String, OfferTerm> reserved;

    public Map<String, OfferTerm> getOnDemand() {
        return onDemand;
    }

    public Map<String, OfferTerm> getReserved() {
        return reserved;
    }
}
