package com.sequenceiq.cloudbreak.common.cost.model;

public class RegionEmissionFactor {

    private String region;

    // tCo2/h/kwh
    private double co2e;

    private String location;

    private String cloud;

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public double getCo2e() {
        return co2e;
    }

    public void setCo2e(double co2e) {
        this.co2e = co2e;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getCloud() {
        return cloud;
    }

    public void setCloud(String cloud) {
        this.cloud = cloud;
    }

    @Override
    public String toString() {
        return "RegionEmissionFactor{" +
                "region='" + region + '\'' +
                ", co2e=" + co2e +
                ", location='" + location + '\'' +
                ", cloud='" + cloud + '\'' +
                '}';
    }
}
