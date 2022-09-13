package com.sequenceiq.cloudbreak.common.cost;

import java.io.Serializable;

public class RealTimeCost implements Serializable {

    private String envCrn;

    private String resourceCrn;

    private Double hourlyProviderUsd;

    private Double hourlyClouderaUsd;

    private Double hourlyCO2;

    public RealTimeCost() {

    }

    public RealTimeCost(String envCrn, String resourceCrn, Double hourlyProviderUsd, Double hourlyClouderaUsd, Double hourlyCO2) {
        this.envCrn = envCrn;
        this.resourceCrn = resourceCrn;
        this.hourlyProviderUsd = hourlyProviderUsd;
        this.hourlyClouderaUsd = hourlyClouderaUsd;
        this.hourlyCO2 = hourlyCO2;
    }

    public String getEnvCrn() {
        return envCrn;
    }

    public void setEnvCrn(String envCrn) {
        this.envCrn = envCrn;
    }

    public String getResourceCrn() {
        return resourceCrn;
    }

    public void setResourceCrn(String resourceCrn) {
        this.resourceCrn = resourceCrn;
    }

    public Double getHourlyProviderUsd() {
        return hourlyProviderUsd;
    }

    public void setHourlyProviderUsd(Double hourlyProviderUsd) {
        this.hourlyProviderUsd = hourlyProviderUsd;
    }

    public Double getHourlyClouderaUsd() {
        return hourlyClouderaUsd;
    }

    public void setHourlyClouderaUsd(Double hourlyClouderaUsd) {
        this.hourlyClouderaUsd = hourlyClouderaUsd;
    }

    public Double getHourlyCO2() {
        return hourlyCO2;
    }

    public void setHourlyCO2(Double hourlyCO2) {
        this.hourlyCO2 = hourlyCO2;
    }

    public RealTimeCost add(RealTimeCost o1) {
        this.hourlyProviderUsd += o1.hourlyProviderUsd;
        this.hourlyClouderaUsd += o1.hourlyClouderaUsd;
        this.hourlyCO2 += o1.hourlyCO2;
        return this;
    }
}
