package com.sequenceiq.cloudbreak.api.model.flex;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class FlexUsageComponentInstanceJson {

    private String guid;

    private String region;

    private String provider;

    private String flexSubscriptionId;

    private String creationTime;

    private String usageDate;

    private Integer peakUsage;

    public String getGuid() {
        return guid;
    }

    public void setGuid(String guid) {
        this.guid = guid;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getFlexSubscriptionId() {
        return flexSubscriptionId;
    }

    public void setFlexSubscriptionId(String flexSubscriptionId) {
        this.flexSubscriptionId = flexSubscriptionId;
    }

    public String getCreationTime() {
        return creationTime;
    }

    public void setCreationTime(String creationTime) {
        this.creationTime = creationTime;
    }

    public String getUsageDate() {
        return usageDate;
    }

    public void setUsageDate(String usageDate) {
        this.usageDate = usageDate;
    }

    public Integer getPeakUsage() {
        return peakUsage;
    }

    public void setPeakUsage(Integer peakUsage) {
        this.peakUsage = peakUsage;
    }
}
