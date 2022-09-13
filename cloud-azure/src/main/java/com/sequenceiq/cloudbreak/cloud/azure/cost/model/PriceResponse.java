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

    public PriceResponse() {
    }

    public PriceResponse(String billingCurrency, String customerEntityId, String customerEntityType, List<PriceDetails> items, String nextPageLink, int count) {
        this.billingCurrency = billingCurrency;
        this.customerEntityId = customerEntityId;
        this.customerEntityType = customerEntityType;
        this.items = items;
        this.nextPageLink = nextPageLink;
        this.count = count;
    }

    public String getBillingCurrency() {
        return billingCurrency;
    }

    public void setBillingCurrency(String billingCurrency) {
        this.billingCurrency = billingCurrency;
    }

    public String getCustomerEntityId() {
        return customerEntityId;
    }

    public void setCustomerEntityId(String customerEntityId) {
        this.customerEntityId = customerEntityId;
    }

    public String getCustomerEntityType() {
        return customerEntityType;
    }

    public void setCustomerEntityType(String customerEntityType) {
        this.customerEntityType = customerEntityType;
    }

    public List<PriceDetails> getItems() {
        return items;
    }

    public void setItems(List<PriceDetails> items) {
        this.items = items;
    }

    public Object getNextPageLink() {
        return nextPageLink;
    }

    public void setNextPageLink(String nextPageLink) {
        this.nextPageLink = nextPageLink;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }
}
