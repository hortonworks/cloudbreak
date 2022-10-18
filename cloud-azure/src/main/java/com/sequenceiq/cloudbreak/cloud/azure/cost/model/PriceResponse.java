package com.sequenceiq.cloudbreak.cloud.azure.cost.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PriceResponse {

    @JsonProperty("BillingCurrency")
    private String billingCurrency;

    @JsonProperty("CustomerEntityId")
    private String customerEntityId;

    @JsonProperty("CustomerEntityType")
    private String customerEntityType;

    @JsonProperty("Items")
    private List<PriceDetails> items;

    @JsonProperty("NextPageLink")
    private String nextPageLink;

    @JsonProperty("Count")
    private int count;

    public String getBillingCurrency() {
        return billingCurrency;
    }

    public String getCustomerEntityId() {
        return customerEntityId;
    }

    public String getCustomerEntityType() {
        return customerEntityType;
    }

    public List<PriceDetails> getItems() {
        return items;
    }

    public Object getNextPageLink() {
        return nextPageLink;
    }

    public int getCount() {
        return count;
    }
}
