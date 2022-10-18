package com.sequenceiq.cloudbreak.cloud.aws.common.cost.model;

import java.util.Date;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class OfferTerm {
    private Map<String, PriceDimension> priceDimensions;

    private String sku;

    private Date effectiveDate;

    private String offerTermCode;

    private Map<String, Object> termAttributes;

    public Map<String, PriceDimension> getPriceDimensions() {
        return priceDimensions;
    }

    public String getSku() {
        return sku;
    }

    public Date getEffectiveDate() {
        return effectiveDate;
    }

    public String getOfferTermCode() {
        return offerTermCode;
    }

    public Map<String, Object> getTermAttributes() {
        return termAttributes;
    }
}