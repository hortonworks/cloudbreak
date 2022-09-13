package com.sequenceiq.cloudbreak.common.cost;

import java.io.Serializable;

public class RealTimeCost implements Serializable {

    private String envCrn;

    private String resourceName;

    private String type;

    private double hourlyProviderUsd;

    private double hourlyClouderaUsd;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getEnvCrn() {
        return envCrn;
    }

    public void setEnvCrn(String envCrn) {
        this.envCrn = envCrn;
    }

    public String getResourceName() {
        return resourceName;
    }

    public void setResourceName(String resourceName) {
        this.resourceName = resourceName;
    }

    public double getHourlyProviderUsd() {
        return hourlyProviderUsd;
    }

    public void setHourlyProviderUsd(double hourlyProviderUsd) {
        this.hourlyProviderUsd = hourlyProviderUsd;
    }

    public double getHourlyClouderaUsd() {
        return hourlyClouderaUsd;
    }

    public void setHourlyClouderaUsd(double hourlyClouderaUsd) {
        this.hourlyClouderaUsd = hourlyClouderaUsd;
    }
}
