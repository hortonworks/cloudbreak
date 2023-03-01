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

    public OfferTerm() {
    }

    public OfferTerm(Map<String, PriceDimension> priceDimensions, String sku, Date effectiveDate, String offerTermCode, Map<String, Object> termAttributes) {
        this.priceDimensions = priceDimensions;
        this.sku = sku;
        this.effectiveDate = effectiveDate;
        this.offerTermCode = offerTermCode;
        this.termAttributes = termAttributes;
    }

    public Map<String, PriceDimension> getPriceDimensions() {
        return priceDimensions;
    }

    public void setPriceDimensions(Map<String, PriceDimension> priceDimensions) {
        this.priceDimensions = priceDimensions;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public Date getEffectiveDate() {
        return effectiveDate;
    }

    public void setEffectiveDate(Date effectiveDate) {
        this.effectiveDate = effectiveDate;
    }

    public String getOfferTermCode() {
        return offerTermCode;
    }

    public void setOfferTermCode(String offerTermCode) {
        this.offerTermCode = offerTermCode;
    }

    public Map<String, Object> getTermAttributes() {
        return termAttributes;
    }

    public void setTermAttributes(Map<String, Object> termAttributes) {
        this.termAttributes = termAttributes;
    }

    @Override
    public String toString() {
        return "OfferTerm{" +
                "priceDimensions=" + priceDimensions +
                ", sku='" + sku + '\'' +
                ", effectiveDate=" + effectiveDate +
                ", offerTermCode='" + offerTermCode + '\'' +
                ", termAttributes=" + termAttributes +
                '}';
    }
}