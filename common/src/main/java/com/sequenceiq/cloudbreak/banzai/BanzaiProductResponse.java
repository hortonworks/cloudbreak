package com.sequenceiq.cloudbreak.banzai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class BanzaiProductResponse {

    private String type;

    private Double onDemandPrice;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Double getOnDemandPrice() {
        return onDemandPrice;
    }

    public void setOnDemandPrice(Double onDemandPrice) {
        this.onDemandPrice = onDemandPrice;
    }
}
