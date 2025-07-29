package com.sequenceiq.cloudbreak.cloud.model;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SkuAttributes implements Serializable {

    private String sku;

    private String ipAllocationMethod;

    private final Class<SkuAttributes> attributeType = SkuAttributes.class;

    public SkuAttributes() {
    }

    @JsonCreator
    public SkuAttributes(
            @JsonProperty("sku") String sku,
            @JsonProperty("ipAllocationMethod") String ipAllocationMethod) {
        this.sku = sku;
        this.ipAllocationMethod = ipAllocationMethod;
    }

    /**
     * Needed for serialization
     * @return class of the current enum
     */
    public Class<SkuAttributes> getAttributeType() {
        return attributeType;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public String getIpAllocationMethod() {
        return ipAllocationMethod;
    }

    public void setIpAllocationMethod(String ipAllocationMethod) {
        this.ipAllocationMethod = ipAllocationMethod;
    }

    @Override
    public String toString() {
        return "SkuAttributes{" +
                "sku='" + sku + '\'' +
                ", ipAllocationMethod='" + ipAllocationMethod + '\'' +
                '}';
    }
}