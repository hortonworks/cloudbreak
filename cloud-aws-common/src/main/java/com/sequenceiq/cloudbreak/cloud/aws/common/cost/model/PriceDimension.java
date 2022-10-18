package com.sequenceiq.cloudbreak.cloud.aws.common.cost.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class PriceDimension {

    private String unit;

    private String endRange;

    private String description;

    private List<Object> appliesTo;

    private String rateCode;

    private String beginRange;

    private PricePerUnit pricePerUnit;

    public String getUnit() {
        return unit;
    }

    public String getEndRange() {
        return endRange;
    }

    public String getDescription() {
        return description;
    }

    public List<Object> getAppliesTo() {
        return appliesTo;
    }

    public String getRateCode() {
        return rateCode;
    }

    public String getBeginRange() {
        return beginRange;
    }

    public PricePerUnit getPricePerUnit() {
        return pricePerUnit;
    }
}
