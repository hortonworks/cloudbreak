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

    public PriceDimension() {
    }

    public PriceDimension(String unit, String endRange, String description, List<Object> appliesTo,
            String rateCode, String beginRange, PricePerUnit pricePerUnit) {
        this.unit = unit;
        this.endRange = endRange;
        this.description = description;
        this.appliesTo = appliesTo;
        this.rateCode = rateCode;
        this.beginRange = beginRange;
        this.pricePerUnit = pricePerUnit;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public String getEndRange() {
        return endRange;
    }

    public void setEndRange(String endRange) {
        this.endRange = endRange;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<Object> getAppliesTo() {
        return appliesTo;
    }

    public void setAppliesTo(List<Object> appliesTo) {
        this.appliesTo = appliesTo;
    }

    public String getRateCode() {
        return rateCode;
    }

    public void setRateCode(String rateCode) {
        this.rateCode = rateCode;
    }

    public String getBeginRange() {
        return beginRange;
    }

    public void setBeginRange(String beginRange) {
        this.beginRange = beginRange;
    }

    public PricePerUnit getPricePerUnit() {
        return pricePerUnit;
    }

    public void setPricePerUnit(PricePerUnit pricePerUnit) {
        this.pricePerUnit = pricePerUnit;
    }

    @Override
    public String toString() {
        return "PriceDimension{" +
                "unit='" + unit + '\'' +
                ", endRange='" + endRange + '\'' +
                ", description='" + description + '\'' +
                ", appliesTo=" + appliesTo +
                ", rateCode='" + rateCode + '\'' +
                ", beginRange='" + beginRange + '\'' +
                ", pricePerUnit=" + pricePerUnit +
                '}';
    }
}
