package com.sequenceiq.cloudbreak.cloud.aws.common.cost.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class PricePerUnit {

    @JsonProperty("USD")
    private double usd;

    public PricePerUnit() {
    }

    public PricePerUnit(double usd) {
        this.usd = usd;
    }

    public double getUsd() {
        return usd;
    }

    public void setUsd(double usd) {
        this.usd = usd;
    }

    @Override
    public String toString() {
        return "PricePerUnit{" +
                "usd=" + usd +
                '}';
    }
}
