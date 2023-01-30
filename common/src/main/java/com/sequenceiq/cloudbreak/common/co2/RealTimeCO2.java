package com.sequenceiq.cloudbreak.common.co2;

import java.io.Serializable;

public class RealTimeCO2 implements Serializable {

    private String envCrn;

    private String resourceCrn;

    private String resourceName;

    private String type;

    private double hourlyCO2InGrams;

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

    public String getResourceName() {
        return resourceName;
    }

    public void setResourceName(String resourceName) {
        this.resourceName = resourceName;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public double getHourlyCO2InGrams() {
        return hourlyCO2InGrams;
    }

    public void setHourlyCO2InGrams(double hourlyCO2InGrams) {
        this.hourlyCO2InGrams = hourlyCO2InGrams;
    }
}
