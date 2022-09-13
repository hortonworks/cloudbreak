package com.sequenceiq.cloudbreak.common.cost;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class EnvironmentRealTimeCost implements Serializable {

    private double hourlyProviderUsd;

    private double hourlyClouderaUsd;

    private RealTimeCost freeipa;

    private RealTimeCost datalake;

    private Map<String, RealTimeCost> datahubs;

    public EnvironmentRealTimeCost() {
        datahubs = new HashMap<>();
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

    public RealTimeCost getFreeipa() {
        return freeipa;
    }

    public void setFreeipa(RealTimeCost freeipa) {
        this.freeipa = freeipa;
    }

    public RealTimeCost getDatalake() {
        return datalake;
    }

    public void setDatalake(RealTimeCost datalake) {
        this.datalake = datalake;
    }

    public Map<String, RealTimeCost> getDatahubs() {
        return datahubs;
    }

    public void setDatahubs(Map<String, RealTimeCost> datahubs) {
        this.datahubs = datahubs;
    }

    public EnvironmentRealTimeCost add(String resourceCrn, RealTimeCost realTimeCost) {
        this.hourlyProviderUsd += realTimeCost.getHourlyProviderUsd();
        this.hourlyClouderaUsd += realTimeCost.getHourlyClouderaUsd();

        if (realTimeCost.getType() != null) {
            switch (realTimeCost.getType()) {
                case "FREEIPA":
                    freeipa = realTimeCost;
                    break;
                case "DATALAKE":
                    datalake = realTimeCost;
                    break;
                case "WORKLOAD":
                    datahubs.put(resourceCrn, realTimeCost);
                    break;
                default:
            }
        }
        return this;
    }
}
