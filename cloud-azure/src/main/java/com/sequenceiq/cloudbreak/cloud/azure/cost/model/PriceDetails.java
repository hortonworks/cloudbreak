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

    public PriceDetails() {
    }

    public PriceDetails(String currencyCode, int tierMinimumUnits, double retailPrice, double unitPrice, String armRegionName, String location,
            Date effectiveStartDate, String meterId, String meterName, String productId, String skuId, Object availabilityId, String productName,
            String skuName, String serviceName, String serviceId, String serviceFamily, String unitOfMeasure, String type, boolean primaryMeterRegion,
            String armSkuName) {
        this.currencyCode = currencyCode;
        this.tierMinimumUnits = tierMinimumUnits;
        this.retailPrice = retailPrice;
        this.unitPrice = unitPrice;
        this.armRegionName = armRegionName;
        this.location = location;
        this.effectiveStartDate = effectiveStartDate;
        this.meterId = meterId;
        this.meterName = meterName;
        this.productId = productId;
        this.skuId = skuId;
        this.availabilityId = availabilityId;
        this.productName = productName;
        this.skuName = skuName;
        this.serviceName = serviceName;
        this.serviceId = serviceId;
        this.serviceFamily = serviceFamily;
        this.unitOfMeasure = unitOfMeasure;
        this.type = type;
        this.primaryMeterRegion = primaryMeterRegion;
        this.armSkuName = armSkuName;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
    }

    public int getTierMinimumUnits() {
        return tierMinimumUnits;
    }

    public void setTierMinimumUnits(int tierMinimumUnits) {
        this.tierMinimumUnits = tierMinimumUnits;
    }

    public double getRetailPrice() {
        return retailPrice;
    }

    public void setRetailPrice(double retailPrice) {
        this.retailPrice = retailPrice;
    }

    public double getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(double unitPrice) {
        this.unitPrice = unitPrice;
    }

    public String getArmRegionName() {
        return armRegionName;
    }

    public void setArmRegionName(String armRegionName) {
        this.armRegionName = armRegionName;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public Date getEffectiveStartDate() {
        return effectiveStartDate;
    }

    public void setEffectiveStartDate(Date effectiveStartDate) {
        this.effectiveStartDate = effectiveStartDate;
    }

    public String getMeterId() {
        return meterId;
    }

    public void setMeterId(String meterId) {
        this.meterId = meterId;
    }

    public String getMeterName() {
        return meterName;
    }

    public void setMeterName(String meterName) {
        this.meterName = meterName;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public String getSkuId() {
        return skuId;
    }

    public void setSkuId(String skuId) {
        this.skuId = skuId;
    }

    public Object getAvailabilityId() {
        return availabilityId;
    }

    public void setAvailabilityId(Object availabilityId) {
        this.availabilityId = availabilityId;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getSkuName() {
        return skuName;
    }

    public void setSkuName(String skuName) {
        this.skuName = skuName;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getServiceId() {
        return serviceId;
    }

    public void setServiceId(String serviceId) {
        this.serviceId = serviceId;
    }

    public String getServiceFamily() {
        return serviceFamily;
    }

    public void setServiceFamily(String serviceFamily) {
        this.serviceFamily = serviceFamily;
    }

    public String getUnitOfMeasure() {
        return unitOfMeasure;
    }

    public void setUnitOfMeasure(String unitOfMeasure) {
        this.unitOfMeasure = unitOfMeasure;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public boolean isPrimaryMeterRegion() {
        return primaryMeterRegion;
    }

    public void setPrimaryMeterRegion(boolean primaryMeterRegion) {
        this.primaryMeterRegion = primaryMeterRegion;
    }

    public String getArmSkuName() {
        return armSkuName;
    }

    public void setArmSkuName(String armSkuName) {
        this.armSkuName = armSkuName;
    }
}
