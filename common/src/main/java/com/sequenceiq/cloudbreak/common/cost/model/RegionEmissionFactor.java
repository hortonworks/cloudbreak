package com.sequenceiq.cloudbreak.common.cost.model;

public class RegionEmissionFactor {

    private String region;

    private double co2e;

    private String country;

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

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
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
                ", country='" + country + '\'' +
                ", cloud='" + cloud + '\'' +
                '}';
    }
}
