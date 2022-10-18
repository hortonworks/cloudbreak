package com.sequenceiq.cloudbreak.cloud.azure.cost.model;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PriceDetails {

    private String currencyCode;

    private int tierMinimumUnits;

    private double retailPrice;

    private double unitPrice;

    private String armRegionName;

    private String location;

    private Date effectiveStartDate;

    private String meterId;

    private String meterName;

    private String productId;

    private String skuId;

    private Object availabilityId;

    private String productName;

    private String skuName;

    private String serviceName;

    private String serviceId;

    private String serviceFamily;

    private String unitOfMeasure;

    private String type;

    @JsonProperty("isPrimaryMeterRegion")
    private boolean primaryMeterRegion;

    private String armSkuName;

    public String getCurrencyCode() {
        return currencyCode;
    }

    public int getTierMinimumUnits() {
        return tierMinimumUnits;
    }

    public double getRetailPrice() {
        return retailPrice;
    }

    public double getUnitPrice() {
        return unitPrice;
    }

    public String getArmRegionName() {
        return armRegionName;
    }

    public String getLocation() {
        return location;
    }

    public Date getEffectiveStartDate() {
        return effectiveStartDate;
    }

    public String getMeterId() {
        return meterId;
    }

    public String getMeterName() {
        return meterName;
    }

    public String getProductId() {
        return productId;
    }

    public String getSkuId() {
        return skuId;
    }

    public Object getAvailabilityId() {
        return availabilityId;
    }

    public String getProductName() {
        return productName;
    }

    public String getSkuName() {
        return skuName;
    }

    public String getServiceName() {
        return serviceName;
    }

    public String getServiceId() {
        return serviceId;
    }

    public String getServiceFamily() {
        return serviceFamily;
    }

    public String getUnitOfMeasure() {
        return unitOfMeasure;
    }

    public String getType() {
        return type;
    }

    public boolean isPrimaryMeterRegion() {
        return primaryMeterRegion;
    }

    public String getArmSkuName() {
        return armSkuName;
    }
}
